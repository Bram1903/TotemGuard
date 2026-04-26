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

    // Bungee bundles cloud-bungee + bstats-bungeecord, plus the adventure
    // serializers (BungeeCord has no native adventure — gson comes along
    // transitively). All four lib trees get relocated to keep them off
    // shared-classloader collision paths.
    relocate("org.incendo.cloud", "com.deathmotion.totemguard.common.libs.cloud")
    relocate("io.leangen.geantyref", "com.deathmotion.totemguard.common.libs.geantyref")
    relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")
    relocate("net.kyori", "com.deathmotion.totemguard.common.libs.kyori")
    relocate("com.google.gson", "com.deathmotion.totemguard.common.libs.gson")
}
