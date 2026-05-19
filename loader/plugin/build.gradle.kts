import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import totemguard.build.CompileNativeTask

plugins {
    id("totemguard.java.internal")
    id("totemguard.shadow.loader")
    id("totemguard.runs.paper")
    id("totemguard.manifest-expand")
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "TotemGuard Loader"

val fabricJij: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    implementation(projects.integrity)

    compileOnly(projects.api)
    compileOnly(projects.loader.host)

    compileOnly(libs.snakeyaml)
    compileOnly(libs.gson)

    compileOnly(libs.paper)
    compileOnly(libs.fabric.loader)

    fabricJij(libs.cloud.fabric)
    fabricJij(libs.adventure.platform.fabric)
    fabricJij(libs.fabric.permissions.api)
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("dev")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    manifest {
        attributes["Implementation-Version"] = project.version.toString()
    }
}

evaluationDependsOn(":platforms:paper")

val fabricGlueJar = project(":loader:fabric-glue").tasks.named<Jar>("jar").flatMap { it.archiveFile }
val paperShadowJar = project(":platforms:paper").tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile }

tasks.named<ShadowJar>("shadowJar") {
    dependsOn(":loader:fabric-glue:jar")
    from(zipTree(fabricGlueJar))
    from(fabricJij) {
        into("META-INF/jars")
    }
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

paperRuns {
    stagedSourceJar.set(paperShadowJar)

    default("1.21.11", java = 21)
    version("1.19.4", java = 17)
    version("1.20.4", java = 17)
    version("1.21.1", java = 21)
    version("1.21.2", java = 21)
    version("1.21.4", java = 21)
    version("26.1.2", java = 25)

    defaultFolia("1.21.11", java = 21)
    folia("1.19.4", java = 17)
    folia("1.20.4", java = 17)
    folia("1.21.1", java = 21)
    folia("1.21.2", java = 21)
    folia("1.21.4", java = 21)
    folia("26.1.2", java = 25)
}
