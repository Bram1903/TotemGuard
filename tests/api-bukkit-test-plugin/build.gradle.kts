plugins {
    totemguard.`java-conventions`
}

repositories {
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.paper)
}

version = "1.0.0-SNAPSHOT"

tasks {
    jar {
        archiveFileName = "TotemGuardAPI-Bukkit-Test.jar"
        archiveClassifier = null
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching(listOf("plugin.yml")) {
            expand(
                "version" to project.version,
            )
        }
    }
}