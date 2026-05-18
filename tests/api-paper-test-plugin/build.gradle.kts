import org.gradle.jvm.tasks.Jar

plugins {
    id("totemguard.java-conventions")
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.paper)
}

version = "1.0.0-SNAPSHOT"

tasks.named<Jar>("jar") {
    archiveFileName = "TotemGuardAPI-Paper-Test.jar"
    archiveClassifier = null
}
