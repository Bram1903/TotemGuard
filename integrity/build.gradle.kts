plugins {
    id("totemguard.java.standard")
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description =
    "Startup-time jar integrity verifier. Shared by the TotemGuard plugin, the loader, and the proxy bridge plugin."
