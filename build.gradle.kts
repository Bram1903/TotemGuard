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
    // Compile-time dependencies
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.lombok)
    compileOnly(libs.betterreload.api)

    // Implementation dependencies (shaded into JAR)
    implementation(libs.expiringmap)
    implementation(libs.configlib.paper)
    implementation(libs.discord.webhooks)

    // Loaded at runtime
    compileOnly(libs.lettuce)
    compileOnly(libs.ebean.core)
    compileOnly(libs.ebean.sqlite)
    compileOnly(libs.ebean.mysql)

    // Annotation processing
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.ebean.processor)

    // Testing dependencies
    testImplementation(libs.ebean.test)
    testImplementation(libs.ebean.sqlite)
    testImplementation(libs.ebean.mysql)
}

group = "com.deathmotion.totemguard"
version = "1.2.0-SNAPSHOT"
description = "TotemGuard"

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-${project.version}.jar"
        archiveClassifier = null

        // ExpiringMap
        relocate("net.jodah.expiringmap", "com.deathmotion.totemguard.shaded.expiringmap")

        // ConfigLib
        relocate("de.exlll.configlib", "com.deathmotion.totemguard.shaded.configlib.configlib")
        relocate("org.snakeyaml.engine", "com.deathmotion.totemguard.shaded.snakeyaml-engine")

        // Discord Webhook
        relocate("club.minnced.discord.webhook", "com.deathmotion.totemguard.shaded.discord-webhook")
        relocate("okhttp3", "com.deathmotion.totemguard.shaded.okhttp3")
        relocate("okio", "com.deathmotion.totemguard.shaded.okio")
        relocate("org.jetbrains.annotations", "com.deathmotion.totemguard.shaded.jetbrains-annotations")

        // PacketEvents
        relocate("net.kyori.adventure.text.serializer.gson", "io.github.retrooper.packetevents.adventure.serializer.gson")

        // Exclude libraries provided by Bukkit/Spigot
        dependencies {
            exclude(dependency("org.slf4j:slf4j-api"))
            exclude(dependency("org.json:json"))
            exclude(dependency("org.yaml:snakeyaml"))
        }

        manifest {
            attributes["Implementation-Version"] = rootProject.version
        }

        minimize()
    }


    assemble {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 17
    }

    withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        inputs.property("version", project.version)
        inputs.property("lettuceVersion", libs.versions.lettuce.get())
        inputs.property("ebeanVersion", libs.versions.ebean.get())

        filesMatching("plugin.yml") {
            expand(
                "version" to rootProject.version,
                "lettuceVersion" to libs.versions.lettuce.get(),
                "ebeanVersion" to libs.versions.ebean.get()
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
        url("https://ci.codemc.io/job/retrooper/job/packetevents/lastSuccessfulBuild/artifact/spigot/build/libs/packetevents-spigot-2.6.1-SNAPSHOT.jar")
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
            url("https://ci.ender.zone/job/EssentialsX/lastSuccessfulBuild/artifact/jars/EssentialsX-2.21.0-dev+141-c7cc1b4.jar")
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
