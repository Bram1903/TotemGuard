plugins {
    totemguard.`java-conventions`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.bstats.velocity)
    implementation(libs.cloud.velocity)
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveFileName = "${rootProject.name}-Velocity-${rootProject.ext["versionNoHash"]}.jar"
        archiveClassifier = null
        destinationDirectory = rootProject.layout.buildDirectory

        exclude("META-INF/maven/**")
    }

    assemble {
        dependsOn(shadowJar)
    }

    runVelocity {
        velocityVersion(libs.versions.velocity.get())

        downloadPlugins {
            url("https://cdn.modrinth.com/data/HYKaKraK/versions/ZjndEJRB/packetevents-velocity-2.11.1.jar")
        }
    }
}
