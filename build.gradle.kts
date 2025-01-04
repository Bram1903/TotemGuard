import java.io.ByteArrayOutputStream

plugins {
    totemguard.`java-conventions`
    `tg-version`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":api"))

    compileOnly(libs.paper)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.configlib.yaml)
    compileOnly(libs.lettuce)
    compileOnly(libs.commandapi)
    compileOnly(libs.expiringmap)
    compileOnly(libs.discord.webhooks)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
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
        inputs.property("configlibVersion", libs.versions.configlib.get())
        inputs.property("discordWebhooksVersion", libs.versions.discord.webhooks.get())
        inputs.property("lettuceVersion", libs.versions.lettuce.get())
        inputs.property("commandapiVersion", libs.versions.commandapi.get())
        inputs.property("expiringmapVersion", libs.versions.expiringmap.get())

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to ext["versionNoHash"],
                "description" to project.description,
                "configlibVersion" to libs.versions.configlib.get(),
                "discordWebhooksVersion" to libs.versions.discord.webhooks.get(),
                "lettuceVersion" to libs.versions.lettuce.get(),
                "commandapiVersion" to libs.versions.commandapi.get(),
                "expiringmapVersion" to libs.versions.expiringmap.get()
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
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.21.0-dev+154-667b0f7.jar")
            url("https://download.luckperms.net/1568/bukkit/loader/LuckPerms-Bukkit-5.4.151.jar")
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