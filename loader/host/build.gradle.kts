plugins {
    java
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "Loader<->inner contract types. Class identity is shared across the loader and inner classloaders via ApiClassInjector, so consumers should not import these types directly."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly(project(":api"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 17
}
