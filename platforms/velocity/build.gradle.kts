plugins {
    id("totemguard.java-conventions")
    id("totemguard.shadow-conventions")
    alias(libs.plugins.run.velocity)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.bstats.velocity)
    implementation(libs.cloud.velocity)
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.get())

        downloadPlugins {
            url("https://cdn.modrinth.com/data/HYKaKraK/versions/ZjndEJRB/packetevents-velocity-2.11.1.jar")
        }
    }
}
