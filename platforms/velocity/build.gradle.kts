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
    shadowJar {
        // Velocity bundles cloud-velocity + bstats-velocity + mysql; Velocity
        // provides adventure natively (stays unbundled and unrelocated).
        relocate("org.incendo.cloud", "com.deathmotion.totemguard.common.libs.cloud")
        relocate("io.leangen.geantyref", "com.deathmotion.totemguard.common.libs.geantyref")
        relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")
        relocate("com.mysql", "com.deathmotion.totemguard.common.libs.mysql")
    }

    runVelocity {
        val version = libs.versions.velocity.get()
        velocityVersion(version)
        runDirectory = rootDir.resolve("run/velocity/$version")

        downloadPlugins {
            url("https://cdn.modrinth.com/data/HYKaKraK/versions/bAnciNVv/packetevents-velocity-2.12.1.jar")
        }
    }
}
