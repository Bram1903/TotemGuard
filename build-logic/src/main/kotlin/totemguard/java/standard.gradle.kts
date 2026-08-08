package totemguard.java

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    java
}

val libs = the<LibrariesForLibs>()

group = rootProject.group
if (version == Project.DEFAULT_VERSION) {
    version = rootProject.version
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    disableAutoTargetJvm()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 17
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}
