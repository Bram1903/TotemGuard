plugins {
    totemguard.`java-conventions`
    `tg-version`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

val compileOnlyDeps: List<Provider<MinimalExternalModuleDependency>> = listOf(
    libs.cloud,
    libs.configlib.yaml,
    libs.lettuce,
    libs.expiringmap,
    libs.ormlite,
    libs.hikaricp
)

val runtimeDeps: List<Provider<MinimalExternalModuleDependency>> = listOf(
    libs.mysql,
    libs.mariadb,
    libs.h2
)

dependencies {
    implementation(project(":api"))
    implementation(libs.commandapi)

    // Provided dependencies
    compileOnly(libs.paper)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Loaded during runtime
    compileOnlyDeps.forEach { compileOnly(it) }
}

group = "com.deathmotion.totemguard"
description = "TotemGuard is a simple anti-cheat that tries to detect players who are using AutoTotem."
val fullVersion = "2.0.5"
val snapshot = true

fun getVersionMeta(includeHash: Boolean): String {
    if (!snapshot) {
        return ""
    }

    var commitHash = ""
    if (includeHash && file(".git").isDirectory) {
        val result = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()

        commitHash = "+$result"
    }

    return "$commitHash-SNAPSHOT"
}
version = "$fullVersion${getVersionMeta(true)}"
ext["versionNoHash"] = "$fullVersion${getVersionMeta(false)}"

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-${ext["versionNoHash"]}.jar"
        archiveClassifier = null
        exclude("META-INF/maven/**")

        relocate("dev.jorel.commandapi", "com.deathmotion.totemguard.shaded.commandapi")

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        dependsOn(generateVersionsFile)
    }

    withType<Test> {
        failOnNoDiscoveredTests = false
    }

    generateVersionsFile {
        packageName = "com.deathmotion.totemguard.util"
    }

    // helper to turn a version-catalog dep into "group:name:version"
    fun Provider<MinimalExternalModuleDependency>.gav(): String {
        val d = get()
        val g = d.module.group
        val a = d.module.name
        val v = d.version
        return if (v.isNullOrBlank()) "$g:$a" else "$g:$a:$v"
    }

    // Two-space indented lines that begin with "- "
    val librariesBlock = "\n" + compileOnlyDeps.joinToString("\n") { """  - "${it.gav()}"""" }
    val runtimeLibrariesBlock = "\n" + runtimeDeps.joinToString("\n") { """  - "${it.gav()}"""" }
    val combinedLibrariesBlock = librariesBlock + runtimeLibrariesBlock

    processResources {
        inputs.properties(
            "version" to ext["versionNoHash"].toString(),
            "description" to (project.description ?: ""),
            "libraries" to combinedLibrariesBlock
        )

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to ext["versionNoHash"].toString(),
                "description" to (project.description ?: ""),
                "libraries" to combinedLibrariesBlock
            )
        }
    }

    // 1.8.8 - 1.16.5 = Java 8
    // 1.17           = Java 16
    // 1.18 - 1.20.4  = Java 17
    // 1-20.5+        = Java 21
    val version = "1.16.5"
    val javaVersion = JavaLanguageVersion.of(21)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true",
        "-DPaper.IgnoreJavaVersion=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://cdn.modrinth.com/data/HYKaKraK/versions/Kee6pozk/packetevents-spigot-2.9.5.jar")
        url("https://cdn.modrinth.com/data/LJNGWSvH/versions/zxYV337v/grimac-bukkit-2.3.72-c10e74c.jar")
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.4.2/ViaVersion-5.4.2.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.4.2/ViaBackwards-5.4.2.jar")
    }

    runServer {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/paper/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.22.0-dev+32-893bae0.jar")
            url("https://download.luckperms.net/1600/bukkit/loader/LuckPerms-Bukkit-5.5.14.jar")
        }

        jvmArgs = jvmArgsExternal
    }

    runPaper.folia.registerTask {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/folia/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
        }

        jvmArgs = jvmArgsExternal
    }
}