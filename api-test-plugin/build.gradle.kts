plugins {
    id("java")
    totemguard.`java-conventions`
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.paper)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

group = "com.deathmotion.totemguard.testplugin"
version = "1.0.0-SNAPSHOT"

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        archiveFileName = "APITestPlugin.jar"
        archiveClassifier = null
    }

    assemble {
        dependsOn(shadowJar)
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to project.version,
            )
        }
    }
}