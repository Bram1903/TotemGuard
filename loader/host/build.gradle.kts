plugins {
    id("totemguard.java.standard")
}

version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
description =
    "Loader<->plugin contract types. Class identity is shared across the loader and TotemGuard plugin classloaders via ApiClassInjector, so consumers should not import these types directly."

dependencies {
    compileOnly(projects.api)
}
