package loader

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Builds the JNI defineClass bridge for every supported platform
 * (linux x86_64 / aarch64, windows x86_64 / aarch64, macos aarch64).
 *
 * Cross-compilation uses `zig cc` when available. If zig is not on PATH the task falls
 * back to the host's `cc` and skips foreign targets.
 *
 * Output binaries are written under `outputDir/<os>-<arch>/` and are intended to be
 * checked into the repository so day-to-day builds do not need to re-run this task.
 */
abstract class CompileNativeTask : DefaultTask() {

    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:InputFile
    abstract val linuxJniMd: RegularFileProperty

    @get:InputFile
    abstract val windowsJniMd: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val javaHome: Property<String>

    @get:Inject
    abstract val execOps: ExecOperations

    private data class Target(
        val osTag: String,
        val archTag: String,
        val libName: String,
        val zigTriple: String,
        /** When non-null, names a vendored jni_md.h directory (linux|win32) used in place
         *  of the host JDK's platform-specific include dir. */
        val vendoredJniMd: String?,
    )

    private val targets = listOf(
        Target("linux", "x86_64", "libtotemguard_loader_native.so", "x86_64-linux-gnu", "linux"),
        Target("linux", "aarch64", "libtotemguard_loader_native.so", "aarch64-linux-gnu", "linux"),
        Target("windows", "x86_64", "totemguard_loader_native.dll", "x86_64-windows-gnu", "win32"),
        Target("windows", "aarch64", "totemguard_loader_native.dll", "aarch64-windows-gnu", "win32"),
        Target("macos", "aarch64", "libtotemguard_loader_native.dylib", "aarch64-macos", null),
    )

    @TaskAction
    fun build() {
        val srcFile = sourceFile.get().asFile
        val outRoot = outputDir.get().asFile
        val resolvedJavaHome = resolveJdkRoot(javaHome.get())
        val jniRoot = resolvedJavaHome.resolve("include")
        if (!jniRoot.resolve("jni.h").exists()) {
            throw GradleException("Cannot find jni.h under $jniRoot; point JAVA_HOME at a JDK installation.")
        }

        val hasZig = locateExecutable("zig") != null
        if (!hasZig) {
            logger.warn(
                "compileNative: 'zig' is not on PATH; cross-compilation is unavailable. " +
                    "Install zig (https://ziglang.org/download/) to produce binaries for every supported platform. " +
                    "Falling back to host-only build with 'cc'."
            )
        }

        for (target in targets) {
            val targetDir = outRoot.resolve("${target.osTag}-${target.archTag}")
            targetDir.mkdirs()
            val outFile = targetDir.resolve(target.libName)

            if (hasZig) {
                buildWithZig(srcFile, outFile, target, jniRoot)
            } else if (isHostTarget(target)) {
                buildWithHostCc(srcFile, outFile, target, jniRoot)
            } else {
                logger.lifecycle("compileNative: skipping ${target.osTag}-${target.archTag} (requires zig for cross-compile)")
            }

            // zig cc produces import-libs / pdbs for windows targets; drop them so only
            // the .dll lands under outputDir.
            File(targetDir, "native.lib").delete()
            File(targetDir, "totemguard_loader_native.pdb").delete()
        }
    }

    private fun resolveJdkRoot(home: String): File {
        val root = File(home)
        if (File(root, "include/jni.h").exists()) return root
        val parent = root.parentFile
        if (parent != null && File(parent, "include/jni.h").exists()) return parent
        return root
    }

    private fun jniIncludes(target: Target, jniRoot: File): List<String> {
        val list = mutableListOf("-I${jniRoot.absolutePath}")
        when (target.vendoredJniMd) {
            "linux" -> list.add(0, "-I${linuxJniMd.get().asFile.parentFile.absolutePath}")
            "win32" -> list.add(0, "-I${windowsJniMd.get().asFile.parentFile.absolutePath}")
            null -> {
                // Same-OS build: use the host JDK's platform-specific include dir.
                val osInclude = when (target.osTag) {
                    "linux" -> "linux"
                    "windows" -> "win32"
                    "macos" -> "darwin"
                    else -> ""
                }
                if (osInclude.isNotEmpty()) {
                    list.add("-I${File(jniRoot, osInclude).absolutePath}")
                }
            }
        }
        return list
    }

    private fun buildWithZig(src: File, out: File, target: Target, jniRoot: File) {
        val args = buildList {
            add("zig"); add("cc"); add("-target"); add(target.zigTriple)
            add("-shared")
            if (target.osTag != "windows") add("-fPIC")
            add("-O2")
            addAll(jniIncludes(target, jniRoot))
            add("-o"); add(out.absolutePath)
            add(src.absolutePath)
        }
        try {
            execOps.exec { commandLine(args) }
            logger.lifecycle("compileNative: built ${out.relativeTo(project.rootDir)} via zig")
        } catch (ex: Exception) {
            logger.warn("compileNative: zig build for ${target.osTag}-${target.archTag} failed: $ex")
        }
    }

    private fun buildWithHostCc(src: File, out: File, target: Target, jniRoot: File) {
        val cc = if (target.osTag == "windows") "gcc" else "cc"
        val args = buildList {
            add(cc); add("-shared")
            if (target.osTag != "windows") add("-fPIC")
            add("-O2")
            addAll(jniIncludes(target, jniRoot))
            add("-o"); add(out.absolutePath)
            add(src.absolutePath)
        }
        try {
            execOps.exec { commandLine(args) }
            logger.lifecycle("compileNative: built ${out.relativeTo(project.rootDir)} via $cc (host only)")
        } catch (ex: Exception) {
            logger.warn("compileNative: host build for ${target.osTag}-${target.archTag} failed: $ex")
        }
    }

    private fun isHostTarget(target: Target): Boolean {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val hostOs = when {
            os.contains("linux") -> "linux"
            os.contains("windows") -> "windows"
            os.contains("mac") || os.contains("darwin") -> "macos"
            else -> ""
        }
        val hostArch = when {
            arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
            arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64") -> "x86_64"
            else -> ""
        }
        return target.osTag == hostOs && target.archTag == hostArch
    }

    private fun locateExecutable(name: String): File? {
        val pathEnv = System.getenv("PATH") ?: return null
        val sep = if (System.getProperty("os.name").lowercase().contains("windows")) ";" else ":"
        for (dir in pathEnv.split(sep)) {
            if (dir.isBlank()) continue
            val f = File(dir, name)
            if (f.isFile && f.canExecute()) return f
            val fExe = File(dir, "$name.exe")
            if (fExe.isFile && fExe.canExecute()) return fExe
        }
        return null
    }
}
