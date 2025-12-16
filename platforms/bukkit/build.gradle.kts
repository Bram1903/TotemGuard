plugins {
    totemguard.`java-conventions`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.bstats.bukkit)
    implementation(libs.cloud.paper)
    compileOnly(libs.paper)
    compileOnly(libs.packetevents.spigot)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-Bukkit-${rootProject.ext["versionNoHash"]}.jar"
        archiveClassifier = null
        destinationDirectory = rootProject.layout.buildDirectory
        exclude("META-INF/maven/**")

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    // 1.8.8 - 1.16.5 = Java 8
    // 1.17           = Java 16
    // 1.18 - 1.20.4  = Java 17
    // 1-20.5+        = Java 21
    val version = "1.21.10"
    val javaVersion = JavaLanguageVersion.of(21)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true",
        "-DPaper.IgnoreJavaVersion=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://cdn.modrinth.com/data/HYKaKraK/versions/yu8xYp9k/packetevents-spigot-2.11.0.jar")
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.6.0/ViaVersion-5.6.0.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.6.0/ViaBackwards-5.6.0.jar")
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