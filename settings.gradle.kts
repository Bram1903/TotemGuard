pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.grim.ac/snapshots")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "TotemGuard"

include(":api")
include(":common")
include(":integrity")
include(":bridge:protocol")
include(":bridge:plugin")
include(":loader:host")
include(":loader:plugin")
include(":loader:fabric-glue")
include(":platforms:paper")
include(":platforms:fabric")
include(":tests:api-paper-test-plugin")
