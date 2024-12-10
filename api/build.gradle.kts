plugins {
    id("java")
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