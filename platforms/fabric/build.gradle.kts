import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.java.fabric")
    id("totemguard.shadow.plugin")
    id("totemguard.manifest-expand")
    alias(libs.plugins.fabric.loom)
    id("totemguard.runs.fabric")
}

version = rootProject.version
description = rootProject.description

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    implementation(projects.common)
    implementation(libs.snakeyaml)
    implementation(libs.mysql.jdbc) {
        exclude(group = "org.slf4j")
        exclude(group = "com.google.protobuf")
    }

    implementation(libs.adventure.platform.fabric)
    implementation(libs.cloud.fabric)
    include(libs.cloud.fabric)
    implementation(libs.fabric.permissions.api)
    include(libs.fabric.permissions.api)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier.set("dev")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

tasks.named<ShadowJar>("shadowJar") {
    val devJar = tasks.named<Jar>("jar")
    dependsOn(devJar)
    from(zipTree(devJar.flatMap { it.archiveFile }))

    val libsPrefix = "com.deathmotion.totemguard.common.libs"
    relocate("com.mysql", "$libsPrefix.mysql")
    relocate("org.yaml.snakeyaml", "$libsPrefix.snakeyaml")

    dependencies {
        include(project(":api"))
        include(project(":common"))
        include(project(":integrity"))
        include(project(":bridge:protocol"))
        include(project(":loader:host"))
        include(dependency("io.lettuce:.*"))
        include(dependency("io.netty:.*"))
        include(dependency("io.projectreactor:.*"))
        include(dependency("org.reactivestreams:.*"))
        include(dependency("com.zaxxer:HikariCP:.*"))
        include(dependency("com.mysql:.*"))
        include(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
        include(dependency("com.google.errorprone:.*"))
        include(dependency("org.jspecify:.*"))
        include(dependency("org.yaml:snakeyaml:.*"))
    }
}
