plugins {
    id("java")
    alias(libs.plugins.shadow)
}

group = "com.deathmotion.totemguard.api"
description = "TotemGuardAPI"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)
}

java {
    disableAutoTargetJvm()
    toolchain.languageVersion = JavaLanguageVersion.of(17)

    withJavadocJar()
    withSourcesJar()
}

tasks {
    shadowJar {
        archiveFileName = "${rootProject.name}API.jar"
        archiveClassifier = null
    }

    assemble {
        dependsOn(shadowJar)
    }

    javadoc {
        title = "TotemGuardAPI v${rootProject.ext["versionNoHash"]}"
        options.encoding = Charsets.UTF_8.name()
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }
}