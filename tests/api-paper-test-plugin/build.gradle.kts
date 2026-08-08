plugins {
    id("totemguard.java.internal")
}

version = "1.0.0-SNAPSHOT"

dependencies {
    compileOnly(projects.api)
    compileOnly(libs.paper)
}

tasks.named<Jar>("jar") {
    archiveFileName.set("TotemGuardAPI-Paper-Test.jar")
    archiveClassifier.set(null as String?)
}
