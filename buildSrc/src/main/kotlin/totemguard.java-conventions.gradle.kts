plugins {
    java
}

version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    disableAutoTargetJvm()
}

tasks {
    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 17
    }

    withType<Test> {
        failOnNoDiscoveredTests = false
    }

    defaultTasks("build")

    processResources {
        inputs.properties(
            "version" to rootProject.ext["versionNoHash"].toString()
        )

        filesMatching(listOf("plugin.yml", "velocity-plugin.json")) {
            expand(
                "version" to rootProject.ext["versionNoHash"].toString()
            )
        }
    }
}