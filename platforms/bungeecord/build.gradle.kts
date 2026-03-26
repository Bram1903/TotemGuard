import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.java-conventions")
    id("totemguard.shadow-conventions")
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.bungeecord)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveFileName =
        "${rootProject.name}-Bungee-${
            project.version.toString().replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
        }.jar"
}
