plugins {
    `java-library`
}

group = "com.deathmotion.totemguard.api"
description = "TotemGuardAPI"
version = rootProject.version

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
    jar {
        archiveFileName = "${rootProject.name}API-${rootProject.ext["versionNoHash"]}.jar"
        archiveClassifier = null
    }

    named<Jar>("javadocJar") {
        archiveFileName.set("${rootProject.name}API-${rootProject.ext["versionNoHash"]}-javadoc.jar")
    }

    named<Jar>("sourcesJar") {
        archiveFileName.set("${rootProject.name}API-${rootProject.ext["versionNoHash"]}-sources.jar")
    }

    javadoc {
        title = "TotemGuardAPI v${rootProject.ext["versionNoHash"]}"
        options.encoding = Charsets.UTF_8.name()
        options {
            (this as CoreJavadocOptions).addBooleanOption("Xdoclint:none", true)
        }
    }

    withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
        options.release = 17
    }
}