import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import totemguard.build.withoutSnapshotHash

plugins {
    id("totemguard.java.fabric")
    id("totemguard.shadow.discord")
    id("totemguard.manifest-expand")
    alias(libs.plugins.fabric.loom)
}

version = rootProject.version
description = "Optional Discord bot for TotemGuard (Fabric)."

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    implementation(projects.discord.common)

    // SnakeYAML is not provided by Minecraft, so the bot core's config loader needs it bundled.
    implementation(libs.snakeyaml)

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

    archiveFileName.set("TotemGuard-Discord-Fabric-${project.version.toString().withoutSnapshotHash()}.jar")

    relocate("org.yaml.snakeyaml", "com.deathmotion.totemguard.discord.libs.snakeyaml")

    // The TotemGuard API is provided by the TotemGuard mod (Fabric shares one classloader),
    // so it is deliberately NOT bundled here. Only the bot core + JDA stack are included.
    dependencies {
        include(project(":discord:common"))
        include(dependency("net.dv8tion:JDA:.*"))
        include(dependency("com.squareup.okhttp3:.*"))
        include(dependency("com.squareup.okio:.*"))
        include(dependency("org.jetbrains.kotlin:.*"))
        include(dependency("com.neovisionaries:nv-websocket-client:.*"))
        include(dependency("org.apache.commons:commons-collections4:.*"))
        include(dependency("net.sf.trove4j:.*"))
        include(dependency("com.fasterxml.jackson.core:.*"))
        include(dependency("org.yaml:snakeyaml:.*"))
    }
}
