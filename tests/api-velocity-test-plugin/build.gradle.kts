import org.gradle.jvm.tasks.Jar

plugins {
    id("totemguard.java-conventions")
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
}

version = "1.0.0-SNAPSHOT"

tasks.named<Jar>("jar") {
    archiveFileName = "TotemGuardAPI-Velocity-Test.jar"
    archiveClassifier = null
}
