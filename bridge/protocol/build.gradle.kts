plugins {
    id("totemguard.java.standard")
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description = "Shared wire format between TotemGuard and TotemGuard-Bridge."
