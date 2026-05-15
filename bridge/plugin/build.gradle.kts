plugins {
    id("totemguard.java-conventions")
    id("totemguard.proxy-shadow-conventions")
    alias(libs.plugins.run.velocity)
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description =
    "Optional proxy-side bridge that improves TotemGuard's player-presence accuracy and powers same-proxy /tg teleport."

tasks.shadowJar {
    archiveFileName.set("TotemGuard-Bridge-${project.version}.jar")
}

dependencies {
    implementation(project(":integrity"))
    implementation(project(":bridge:protocol"))
    implementation(libs.lettuce) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    compileOnly(libs.velocity)
    compileOnly(libs.bungeecord)
}

tasks {
    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
        runDirectory = rootDir.resolve("run/velocity/")
    }
}