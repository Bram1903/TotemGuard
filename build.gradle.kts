import java.io.ByteArrayOutputStream

plugins {
    totemguard.`java-conventions`
    `tg-version`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":api"))
    implementation(files("libs/libby-core.jar"))
    implementation(files("libs/libby-bukkit.jar"))

    // Provided dependencies
    compileOnly(libs.paper)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Loaded during runtime
    compileOnly(libs.configlib.yaml)
    compileOnly(libs.lettuce)
    compileOnly(libs.commandapi)
    compileOnly(libs.expiringmap)
    compileOnly(libs.discord.webhooks)
    compileOnly(libs.ormlite)
}

group = "com.deathmotion.totemguard"
description = "TotemGuard is a simple anti-cheat that tries to detect players who are using AutoTotem."
val fullVersion = "2.0.0"
val snapshot = true

fun getVersionMeta(includeHash: Boolean): String {
    if (!snapshot) {
        return ""
    }
    var commitHash = ""
    if (includeHash && file(".git").isDirectory) {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
        }
        commitHash = "+${stdout.toString().trim()}"
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

        relocate("com.alessiodp.libby", "com.deathmotion.totemguard.shaded.libby")

        relocate("de.exlll.configlib", "com.deathmotion.totemguard.libs.configlib")
        relocate("dev.jorel.commandapi", "com.deathmotion.totemguard.libs.commandapi")
        relocate("io.lettuce.core", "com.deathmotion.totemguard.libs.lettuce")
        relocate("net.jodah.expiringmap", "com.deathmotion.totemguard.libs.expiringmap")

        // Database libraries
        relocate("com.j256.ormlite", "com.deathmotion.totemguard.libs.ormlite")
        relocate("com.mysql", "com.deathmotion.totemguard.libs.mysql")
        relocate("org.mariadb", "com.deathmotion.totemguard.libs.mariadb")
        relocate("org.h2", "com.deathmotion.totemguard.libs.h2")
    }

    assemble {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        dependsOn(generateVersionsFile)
    }

    generateVersionsFile {
        packageName = "com.deathmotion.totemguard.util"
    }

    processResources {
        inputs.property("version", ext["versionNoHash"])
        inputs.property("description", project.description)

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to ext["versionNoHash"],
                "description" to project.description
            )
        }
    }

    // 1.8.8 - 1.16.5 = Java 8
    // 1.17           = Java 16
    // 1.18 - 1.20.4  = Java 17
    // 1-20.5+        = Java 21
    val version = "1.21.4"
    val javaVersion = JavaLanguageVersion.of(21)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://ci.codemc.io/job/retrooper/job/packetevents/lastSuccessfulBuild/artifact/spigot/build/libs/packetevents-spigot-2.7.1-SNAPSHOT.jar")
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.2.1/ViaVersion-5.2.1.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.2.1/ViaBackwards-5.2.1.jar")
    }

    runServer {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/paper/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.21.0-dev+173-5a839c4.jar")
            url("https://download.luckperms.net/1570/bukkit/loader/LuckPerms-Bukkit-5.4.153.jar")
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