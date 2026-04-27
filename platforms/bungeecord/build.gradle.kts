import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.java-conventions")
    id("totemguard.shadow-conventions")
}

dependencies {
    implementation(project(":common"))
    implementation(libs.bstats.bungeecord)
    implementation(libs.cloud.bungee)
    implementation(libs.bundles.adventure.serializers)
    compileOnly(libs.bungeecord)
    compileOnly(libs.bundles.adventure)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName =
        "${rootProject.name}-Bungee-${
            project.version.toString().replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
        }.jar"

    relocate("net.kyori", "com.deathmotion.totemguard.common.libs.kyori")
}
