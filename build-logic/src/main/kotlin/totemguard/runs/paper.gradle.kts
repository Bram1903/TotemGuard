package totemguard.runs

import totemguard.build.PaperRunDefaults
import totemguard.build.PaperRunSpec
import totemguard.build.PaperRunsExtension
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.pluginsapi.DownloadPluginsSpec
import xyz.jpenilla.runtask.pluginsapi.PluginDownloadService
import xyz.jpenilla.runtask.service.DownloadsAPIService
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("xyz.jpenilla.run-paper")
}

val paperRuns = extensions.create<PaperRunsExtension>("paperRuns")
paperRuns.stagedPluginDir.convention(PaperRunDefaults.DEFAULT_STAGED_PLUGIN_DIR)

runPaper.disablePluginJarDetection()

val javaToolchainService = extensions.getByType<JavaToolchainService>()

fun RunServer.applyPaperRunSpec(
    spec: PaperRunSpec,
    sharedSpec: DownloadPluginsSpec,
    runDirName: String,
    extraPluginUrls: List<String>,
) {
    minecraftVersion(spec.minecraftVersion)
    runDirectory.set(layout.projectDirectory.dir("run/$runDirName/${spec.minecraftVersion}"))
    javaLauncher.set(
        javaToolchainService.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(spec.javaVersion))
        }
    )
    jvmArgs = paperRuns.sharedJvmArgs.toList()
    downloadPlugins {
        from(sharedSpec)
        extraPluginUrls.forEach { url(it) }
    }
    pluginJars(project.tasks.named("shadowJar", org.gradle.jvm.tasks.Jar::class.java).flatMap { it.archiveFile })
    attachStagedSourceJar()
}

fun RunServer.attachStagedSourceJar() {
    val source = paperRuns.stagedSourceJar
    if (!source.isPresent) return
    val pluginDirInRun = paperRuns.stagedPluginDir
    inputs.file(source)
    val destination = runDirectory.zip(pluginDirInRun) { runDir, dir -> runDir.dir(dir) }
    doFirst {
        val target = destination.get().asFile.toPath()
        Files.createDirectories(target)
        Files.newDirectoryStream(target, "*.jar").use { entries ->
            entries.forEach { Files.delete(it) }
        }
        val sourcePath = source.get().asFile.toPath()
        Files.copy(
            sourcePath,
            target.resolve(sourcePath.fileName.toString()),
            StandardCopyOption.REPLACE_EXISTING,
        )
        logger.lifecycle("Staged ${sourcePath.fileName} -> $target for /tgloader LOCAL source.")
    }
}

fun TaskContainer.registerFoliaRun(name: String, configure: RunServer.() -> Unit) =
    register<RunServer>(name) {
        group = "run paper"
        downloadsApiService.convention(DownloadsAPIService.folia(project))
        pluginDownloadService.convention(PluginDownloadService.paper(project))
        displayName.set("Folia")
        configure()
    }

fun versionTaskName(prefix: String, mcVersion: String): String =
    "${prefix}_${mcVersion.replace('.', '_')}"

afterEvaluate {
    val sharedSpec: DownloadPluginsSpec = runPaper.downloadPluginsSpec {
        paperRuns.sharedPluginUrls.forEach { url(it) }
    }
    val paperOnlyPlugins = paperRuns.paperOnlyPluginUrls.toList()

    val allPaperSpecs = listOfNotNull(paperRuns.defaultRun) + paperRuns.extraRuns
    val allFoliaSpecs = listOfNotNull(paperRuns.defaultFoliaRun) + paperRuns.extraFoliaRuns

    paperRuns.defaultRun?.let { spec ->
        tasks.named<RunServer>("runServer") {
            applyPaperRunSpec(spec, sharedSpec, runDirName = "paper", extraPluginUrls = paperOnlyPlugins)
        }
    }

    allPaperSpecs.forEach { spec ->
        tasks.register<RunServer>(versionTaskName("runServer", spec.minecraftVersion)) {
            group = "run paper"
            description = "Run a Paper ${spec.minecraftVersion} server for plugin testing (Java ${spec.javaVersion})."
            applyPaperRunSpec(spec, sharedSpec, runDirName = "paper", extraPluginUrls = paperOnlyPlugins)
        }
    }

    paperRuns.defaultFoliaRun?.let { spec ->
        runPaper.folia.registerTask {
            description = "Run a Folia ${spec.minecraftVersion} server for plugin testing (Java ${spec.javaVersion})."
            applyPaperRunSpec(spec, sharedSpec, runDirName = "folia", extraPluginUrls = emptyList())
        }
    }

    allFoliaSpecs.forEach { spec ->
        tasks.registerFoliaRun(versionTaskName("runFolia", spec.minecraftVersion)) {
            description = "Run a Folia ${spec.minecraftVersion} server for plugin testing (Java ${spec.javaVersion})."
            applyPaperRunSpec(spec, sharedSpec, runDirName = "folia", extraPluginUrls = emptyList())
        }
    }
}
