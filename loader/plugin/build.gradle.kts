import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import loader.CompileNativeTask
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("totemguard.java-conventions")
    id("totemguard.loader-shadow-conventions")
    alias(libs.plugins.run.paper)
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "TotemGuard Loader"

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier = "dev"
    destinationDirectory = layout.buildDirectory.dir("libs")
    manifest {
        attributes["Implementation-Version"] = project.version.toString()
    }
}

dependencies {
    implementation(project(":integrity"))

    compileOnly(project(":api"))
    compileOnly(project(":loader:host"))

    compileOnly(libs.snakeyaml)
    compileOnly("com.google.code.gson:gson:2.11.0")

    compileOnly(libs.paper)
    compileOnly(libs.fabric.loader)
}

tasks.named<ShadowJar>("shadowJar") {
    dependsOn(":loader:fabric-glue:jar")
    from(provider {
        val glueJar = project(":loader:fabric-glue").tasks.named<Jar>("jar").get().archiveFile.get().asFile
        zipTree(glueJar)
    })
}

tasks.register<CompileNativeTask>("compileNative") {
    description = "Builds the JNI defineClass bridge for every supported platform via zig cc. " +
            "Requires zig on PATH; the same toolchain is used on every host OS for reproducible binaries."
    group = "build"

    sourceFile.set(file("src/main/c/native.c"))
    linuxJniMd.set(file("src/main/c/jni_md/linux/jni_md.h"))
    windowsJniMd.set(file("src/main/c/jni_md/win32/jni_md.h"))
    darwinJniMd.set(file("src/main/c/jni_md/darwin/jni_md.h"))
    outputDir.set(file("src/main/resources/natives"))
    javaHome.set(System.getProperty("java.home"))
}

tasks {
    // 1.8.8 - 1.16.5 = Java 8
    // 1.17           = Java 16
    // 1.18 - 1.20.4  = Java 17
    // 1-20.5+        = Java 21
    val mcVersion = "1.21.11"
    val javaVersion = JavaLanguageVersion.of(21)

    val jvmArgsExternal = listOf(
        "-Dcom.mojang.eula.agree=true",
        "-DPaper.IgnoreJavaVersion=true"
    )

    val sharedPlugins = runPaper.downloadPluginsSpec {
        url("https://cdn.modrinth.com/data/HYKaKraK/versions/ap8qHs7D/packetevents-spigot-2.12.1.jar")
        url("https://github.com/ViaVersion/ViaVersion/releases/download/5.9.1/ViaVersion-5.9.1.jar")
        url("https://github.com/ViaVersion/ViaBackwards/releases/download/5.9.1/ViaBackwards-5.9.1.jar")
        url("https://github.com/PlaceholderAPI/PlaceholderAPI/releases/download/2.12.2/PlaceholderAPI-2.12.2.jar")
    }

    runServer {
        minecraftVersion(mcVersion)
        runDirectory = rootDir.resolve("loader/plugin/run/paper/$mcVersion")

        javaLauncher = project.javaToolchains.launcherFor {
            languageVersion = javaVersion
        }

        downloadPlugins {
            from(sharedPlugins)
            url("https://cdn.modrinth.com/data/hXiIvTyT/versions/Oa9ZDzZq/EssentialsX-2.21.2.jar")
            url("https://download.luckperms.net/1631/bukkit/loader/LuckPerms-Bukkit-5.5.42.jar")
        }

        jvmArgs = jvmArgsExternal

        val paperShadow = project(":platforms:paper").tasks.named("shadowJar")
        dependsOn(paperShadow)
        doFirst {
            val localDir = runDirectory.get().asFile.toPath().resolve("plugins/TotemGuard-Loader/local")
            Files.createDirectories(localDir)
            Files.newDirectoryStream(localDir, "*.jar").use { entries ->
                entries.forEach { Files.delete(it) }
            }
            val source = paperShadow.get().outputs.files.singleFile.toPath()
            val destination = localDir.resolve(source.fileName.toString())
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            logger.lifecycle("Copied ${source.fileName} -> $destination for /tgloader LOCAL source.")
        }
    }
}
