plugins {
    id("totemguard.java.internal")
    id("totemguard.shadow.proxy")
    id("totemguard.runs.velocity")
    id("totemguard.manifest-expand")
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description =
    "Optional proxy-side bridge that improves TotemGuard's player-presence accuracy and powers same-proxy /tg teleport."

dependencies {
    implementation(projects.integrity)
    implementation(projects.bridge.protocol)
    implementation(libs.lettuce) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    compileOnly(libs.velocity)
    compileOnly(libs.bungeecord)
}

tasks.shadowJar {
    archiveFileName.set("TotemGuard-Bridge-${project.version}.jar")
}
