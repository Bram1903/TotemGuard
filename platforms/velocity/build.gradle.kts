import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("totemguard.java-conventions")
    id("totemguard.shadow-conventions")
    alias(libs.plugins.run.velocity)
}

dependencies {
    implementation(project(":common"))
    implementation(libs.bstats.velocity)
    implementation(libs.cloud.velocity)
    implementation(libs.mysql.jdbc) {
        exclude(group = "org.slf4j")
        exclude(group = "com.google.protobuf")
    }
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

tasks {
    // Relocate to avoid clashing with other proxy plugins bundling the driver.
    withType<ShadowJar>().configureEach {
        relocate("com.mysql", "com.deathmotion.totemguard.common.libs.mysql")
    }

    runVelocity {
        val version = libs.versions.velocity.get()
        velocityVersion(version)
        runDirectory = rootDir.resolve("run/velocity/$version")

        downloadPlugins {
            url("https://cdn.modrinth.com/data/HYKaKraK/versions/ZjndEJRB/packetevents-velocity-2.11.1.jar")
        }
    }
}
