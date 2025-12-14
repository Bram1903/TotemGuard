plugins {
    totemguard.`java-conventions`
    alias(libs.plugins.shadow)
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

version = "1.0.0-SNAPSHOT"

tasks {
    jar {
        archiveFileName = "TotemGuardAPI-Velocity-Test.jar"
        archiveClassifier = null
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching(listOf("velocity-plugin.json", "velocity-plugin.json")) {
            expand(
                "version" to project.version,
            )
        }
    }
}