import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.java.fabric")
    id("totemguard.manifest-expand")
    id("totemguard.loader-stage-platform")
    alias(libs.plugins.fabric.loom)
    id("totemguard.runs.fabric")
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "TotemGuard Loader Fabric glue (CommandRegistrationCallback + CommandSourceStack hooks)"

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    compileOnly(libs.slf4j.api)

    implementation(projects.api)
    implementation(projects.loader.host)
    implementation(projects.loader.plugin)
    implementation(projects.integrity)

    implementation(libs.cloud.fabric)
    implementation(libs.adventure.platform.fabric)
    implementation(libs.fabric.permissions.api)
}

evaluationDependsOn(":platforms:fabric")

stageLoaderJar {
    val fabricShadow = project(":platforms:fabric").tasks.named<ShadowJar>("shadowJar")
    sourceShadowJar.set(fabricShadow.flatMap { it.archiveFile })
    destinationDir.set(layout.projectDirectory.dir("run/config/totemguard-loader/local"))
    attachToTask.set("runServer")
}
