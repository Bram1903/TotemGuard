plugins {
    java
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "Startup-time jar integrity verifier. Shared by the TotemGuard plugin, the loader, and the proxy bridge plugin."

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
