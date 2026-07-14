package totemguard.runs

import org.gradle.accessors.dm.LibrariesForLibs
import totemguard.build.DownloadModrinthModsTask
import totemguard.build.FabricRunDefaults
import totemguard.build.FabricRunsExtension
import totemguard.build.ModrinthModSpec
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    java
}

val libs = the<LibrariesForLibs>()

val fabricRuns = extensions.create<FabricRunsExtension>("fabricRuns")
fabricRuns.minecraftVersion.convention(libs.versions.minecraft.get())
fabricRuns.loader.convention("fabric")
fabricRuns.runDirectory.convention("run")
fabricRuns.mods.convention(FabricRunDefaults.MOD_SLUGS.map { ModrinthModSpec(it) })

val resolveFabricMods = tasks.register<DownloadModrinthModsTask>("resolveFabricMods") {
    group = "fabric runs"
    description = "Resolve and download Fabric-compatible mods from Modrinth for runServer."
    minecraftVersion.set(fabricRuns.minecraftVersion)
    loader.set(fabricRuns.loader)
    mods.set(fabricRuns.mods)
    cacheDir.set(layout.buildDirectory.dir("fabric-mod-cache"))
}

afterEvaluate {
    val runServer = tasks.findByName("runServer") ?: return@afterEvaluate
    val cacheProvider = resolveFabricMods.flatMap { it.cacheDir }
    val runDirProvider = fabricRuns.runDirectory.map { layout.projectDirectory.dir(it) }

    runServer.dependsOn(resolveFabricMods)
    runServer.doFirst {
        val runDir = runDirProvider.get().asFile.toPath()
        Files.createDirectories(runDir)

        val eula = runDir.resolve("eula.txt")
        if (!Files.exists(eula) || !Files.readString(eula).contains("eula=true")) {
            Files.writeString(eula, "eula=true\n")
        }

        val properties = runDir.resolve("server.properties")
        if (!Files.exists(properties)) {
            Files.writeString(properties, "")
        }

        val target = runDir.resolve("mods")
        Files.createDirectories(target)
        val cache = cacheProvider.get().asFile.toPath()
        if (!Files.exists(cache)) return@doFirst
        Files.newDirectoryStream(cache, "*.jar").use { entries ->
            for (entry in entries) {
                val dest = target.resolve(entry.fileName.toString())
                if (Files.exists(dest)) continue
                Files.copy(entry, dest, StandardCopyOption.REPLACE_EXISTING)
                logger.lifecycle("Staged ${entry.fileName} -> $target")
            }
        }
    }
}
