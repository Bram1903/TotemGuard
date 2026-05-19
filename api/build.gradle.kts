plugins {
    id("totemguard.java.api")
    id("totemguard.tg-version")
    id("totemguard.maven-publish")
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "TotemGuard API"

tgVersion {
    packageName.set("com.deathmotion.totemguard.api.versioning")
    className.set("TGAPIVersions")
}

val artifactBaseName = "${rootProject.name}API"

tasks.named<Jar>("jar") {
    archiveFileName.set("$artifactBaseName-${project.version}.jar")
    archiveClassifier.set(null as String?)
}

tasks.named<Jar>("javadocJar") {
    archiveFileName.set("$artifactBaseName-${project.version}-javadoc.jar")
}

tasks.named<Jar>("sourcesJar") {
    archiveFileName.set("$artifactBaseName-${project.version}-sources.jar")
}

tasks.named<Javadoc>("javadoc") {
    title = "TotemGuard API v${project.version}"
}
