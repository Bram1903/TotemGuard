import java.io.ByteArrayOutputStream

plugins {
    `java-library`
    `maven-publish`
    `tg-version`
    alias(libs.plugins.ebean)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://jitpack.io") }
}

java {
    disableAutoTargetJvm()
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
    implementation(project(":api"))

    // Compile-time dependencies
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.lombok)
    compileOnly(libs.betterreload.api)

    // Loaded at runtime
    compileOnly(libs.lettuce)
    compileOnly(libs.ebean.core)
    compileOnly(libs.ebean.sqlite)
    compileOnly(libs.ebean.mysql)
    compileOnly(libs.expiringmap)
    compileOnly(libs.configlib.paper)
    compileOnly(libs.discord.webhooks)

    // Annotation processing
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.ebean.processor)

    // Testing dependencies
    testImplementation(libs.ebean.test)
    testImplementation(libs.ebean.sqlite)
    testImplementation(libs.ebean.mysql)
}

group = "com.deathmotion.totemguard"
description = "TotemGuard"
val fullVersion = "1.2.1"
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

        relocate("net.kyori.adventure.text.serializer.gson", "io.github.retrooper.packetevents.adventure.serializer.gson")
    }

    assemble {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        dependsOn(generateVersionsFile)
        options.encoding = Charsets.UTF_8.name()
        options.release = 17
    }

    withType<Javadoc> {
        mustRunAfter(generateVersionsFile)
        options.encoding = Charsets.UTF_8.name()
    }

    generateVersionsFile {
        packageName = "com.deathmotion.totemguard.util"
    }

    processResources {
        inputs.property("version", ext["versionNoHash"])
        inputs.property("ebeanVersion", libs.versions.ebean.get())
        inputs.property("configlibVersion", libs.versions.configlib.get())
        inputs.property("discordWebhooksVersion", libs.versions.discord.webhooks.get())
        inputs.property("expiringmapVersion", libs.versions.expiringmap.get())
        inputs.property("lettuceVersion", libs.versions.lettuce.get())

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to ext["versionNoHash"],
                "ebeanVersion" to libs.versions.ebean.get(),
                "configlibVersion" to libs.versions.configlib.get(),
                "discordWebhooksVersion" to libs.versions.discord.webhooks.get(),
                "expiringmapVersion" to libs.versions.expiringmap.get(),
                "lettuceVersion" to libs.versions.lettuce.get()
            )
        }
    }

    defaultTasks("build")

    // 1.8.8 - 1.16.5 = Java 8
    // 1.17           = Java 16
    // 1.18 - 1.20.4  = Java 17
    // 1-20.5+        = Java 21
    val version = "1.21.3"
    val javaVersion = JavaLanguageVersion.of(21)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://ci.codemc.io/job/retrooper/job/packetevents/lastSuccessfulBuild/artifact/spigot/build/libs/packetevents-spigot-2.7.0-SNAPSHOT.jar")
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.1.1/ViaVersion-5.1.1.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.1.1/ViaBackwards-5.1.1.jar")
    }

    runServer {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/paper/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.21.0-dev+149-424816e.jar")
            url("https://ci.lucko.me/job/spark/465/artifact/spark-bukkit/build/libs/spark-1.10.119-bukkit.jar")
            url("https://download.luckperms.net/1561/bukkit/loader/LuckPerms-Bukkit-5.4.146.jar")
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
