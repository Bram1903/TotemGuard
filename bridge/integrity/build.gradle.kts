plugins {
    java
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "Shared startup-time jar integrity verifier used by TotemGuard and TotemGuard-Bridge."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 17
}
