plugins {
    totemguard.`java-conventions`
    `tg-version`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":api"))
    implementation(libs.commandapi)

    // Provided dependencies
    compileOnly(libs.paper)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Loaded during runtime
    compileOnly(libs.configlib.yaml)
    compileOnly(libs.lettuce)
    compileOnly(libs.expiringmap)
    compileOnly(libs.ormlite)
    compileOnly(libs.hikaricp)
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

    processResources {
        inputs.properties(
            "version" to ext["versionNoHash"].toString(),
            "description" to (project.description ?: "")
        )

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to ext["versionNoHash"].toString(),
                "description" to (project.description ?: "")
            )
        }
    }

    // 1.8.8 - 1.16.5 = Java 8
    // 1.17           = Java 16
    // 1.18 - 1.20.4  = Java 17
    // 1-20.5+        = Java 21
    val version = "1.21.8"
    val javaVersion = JavaLanguageVersion.of(21)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://cdn.modrinth.com/data/HYKaKraK/versions/Kee6pozk/packetevents-spigot-2.9.5.jar")
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.5.0/ViaVersion-5.5.0.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.5.0/ViaBackwards-5.5.0.jar")
    }

    runServer {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/paper/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
            url("https://github.com/EssentialsX/Essentials/releases/download/2.21.2/EssentialsX-2.21.2.jar")
            url("https://download.luckperms.net/1602/bukkit/loader/LuckPerms-Bukkit-5.5.15.jar")
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