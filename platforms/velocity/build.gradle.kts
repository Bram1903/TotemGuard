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
            url("https://cdn.modrinth.com/data/HYKaKraK/versions/BMvG9QW5/packetevents-velocity-2.10.0.jar")
        }
    }
}
