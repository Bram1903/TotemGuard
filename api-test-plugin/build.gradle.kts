plugins {
    id("java")
    alias(libs.plugins.shadow)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://jitpack.io") }
}

java {
    disableAutoTargetJvm()
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
    compileOnly(project(":api"))
    compileOnly(libs.paper.api)
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

    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 17
    }

    withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        inputs.property("version", project.version)

        filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
            expand(
                "version" to project.version,
            )
        }
    }

    defaultTasks("build")
}