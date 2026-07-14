/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package totemguard.build

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
 * Cross-compilation always goes through `zig cc` so the build is reproducible regardless
 * of host OS. The host JDK is used only for the platform-independent `jni.h`. The
 * platform-specific `jni_md.h` is vendored under `src/main/c/jni_md/<linux|win32|darwin>/`
 * so a Windows host produces the same macOS binary a Linux host would.
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

    @get:InputFile
    abstract val darwinJniMd: RegularFileProperty

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
        val vendoredJniMd: String,
    )

    private val targets = listOf(
        Target("linux", "x86_64", "libtotemguard_loader_native.so", "x86_64-linux-gnu.2.31", "linux"),
        Target("linux", "aarch64", "libtotemguard_loader_native.so", "aarch64-linux-gnu.2.31", "linux"),
        Target("windows", "x86_64", "totemguard_loader_native.dll", "x86_64-windows-gnu", "win32"),
        Target("windows", "aarch64", "totemguard_loader_native.dll", "aarch64-windows-gnu", "win32"),
        Target("macos", "aarch64", "libtotemguard_loader_native.dylib", "aarch64-macos", "darwin"),
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

        if (locateExecutable("zig") == null) {
            throw GradleException(
                "compileNative requires 'zig' on PATH. Install zig (https://ziglang.org/download/) " +
                        "so the build produces identical binaries on every host OS."
            )
        }

        for (target in targets) {
            val targetDir = outRoot.resolve("${target.osTag}-${target.archTag}")
            targetDir.mkdirs()
            val outFile = targetDir.resolve(target.libName)

            buildWithZig(srcFile, outFile, target, jniRoot)

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

    private fun vendoredJniMdDir(target: Target): File = when (target.vendoredJniMd) {
        "linux" -> linuxJniMd.get().asFile.parentFile
        "win32" -> windowsJniMd.get().asFile.parentFile
        "darwin" -> darwinJniMd.get().asFile.parentFile
        else -> error("unknown vendored jni_md directory: ${target.vendoredJniMd}")
    }

    private fun buildWithZig(src: File, out: File, target: Target, jniRoot: File) {
        val args = buildList {
            add("zig"); add("cc"); add("-target"); add(target.zigTriple)
            add("-shared")
            if (target.osTag != "windows") add("-fPIC")
            add("-O2")
            add("-I${vendoredJniMdDir(target).absolutePath}")
            add("-I${jniRoot.absolutePath}")
            add("-o"); add(out.absolutePath)
            add(src.absolutePath)
        }
        execOps.exec { commandLine(args) }
        logger.lifecycle("compileNative: built ${out.relativeTo(project.rootDir)} via zig")
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
