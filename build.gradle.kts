plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.ebean)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.codemc.io/repository/maven-releases/")
    }
    maven {
        url = uri("https://jitpack.io")
    }
}

java {
    disableAutoTargetJvm()
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.discord.webhooks)
    compileOnly(libs.configlib.yaml)
    compileOnly(libs.configlib.paper)
    compileOnly(libs.lombok)
    compileOnly(libs.expiringmap)
    compileOnly(libs.betterreload.api)
    compileOnly(libs.ebean.core)
    compileOnly(libs.ebean.sqlite)
    compileOnly(libs.ebean.mysql)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.ebean.processor)
    testImplementation(libs.ebean.test)
    testImplementation(libs.ebean.sqlite)
    testImplementation(libs.ebean.mysql)
}

group = "com.deathmotion.totemguard"
version = "1.0.1-SNAPSHOT"
description = "TotemGuard"

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-${project.version}.jar"
        archiveClassifier = null

        manifest {
            attributes["Implementation-Version"] = rootProject.version
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 17
    }

    withType<Javadoc>() {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        inputs.property("version", project.version)
        inputs.property("ebeanVersion", libs.versions.ebean.get())
        inputs.property("configlibVersion", libs.versions.configlib.get())
        inputs.property("discordWebhooksVersion", libs.versions.discord.webhooks.get())
        inputs.property("expiringmapVersion", libs.versions.expiringmap.get())

        filesMatching("plugin.yml") {
            expand(
                "version" to rootProject.version,
                "ebeanVersion" to libs.versions.ebean.get(),
                "configlibVersion" to libs.versions.configlib.get(),
                "discordWebhooksVersion" to libs.versions.discord.webhooks.get(),
                "expiringmapVersion" to libs.versions.expiringmap.get()
            )
        }
    }

    defaultTasks("build")

    // 1.8.8 - 1.16.5 = Java 8
    // 1.17           = Java 16
    // 1.18 - 1.20.4  = Java 17
    // 1-20.5+        = Java 21
    val version = "1.21.1"
    val javaVersion = JavaLanguageVersion.of(21)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://ci.codemc.io/job/retrooper/job/packetevents/lastSuccessfulBuild/artifact/spigot/build/libs/packetevents-spigot-2.5.1-SNAPSHOT.jar")
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.0.3/ViaVersion-5.0.3.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.0.3/ViaBackwards-5.0.3.jar")
        url("https://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/artifacts/FastAsyncWorldEdit-Bukkit-2.11.3-SNAPSHOT-916.jar")
    }

    runServer {
        minecraftVersion(version)
        runDirectory = rootDir.resolve("run/paper/$version")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.21.0-dev+115-6157668.jar")
            url("https://download.luckperms.net/1552/bukkit/loader/LuckPerms-Bukkit-5.4.137.jar")
            url("https://ci.lucko.me/job/spark/449/artifact/spark-bukkit/build/libs/spark-1.10.103-bukkit.jar")
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
