import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import totemguard.build.withoutSnapshotHash

plugins {
    id("totemguard.java.internal")
    id("totemguard.shadow.discord")
    id("totemguard.manifest-expand")
}

description = "Optional Discord bot for TotemGuard (Paper/Folia)."

dependencies {
    implementation(projects.discord.common)
    compileOnly(projects.api)
    compileOnly(libs.paper)
}

tasks.withType<ShadowJar>().configureEach {
    // The module name is "paper", which the shadow convention would turn into the core
    // plugin's name. Override so the optional jar is unmistakable and never collides.
    archiveFileName.set("TotemGuard-Discord-Paper-${project.version.toString().withoutSnapshotHash()}.jar")
}
