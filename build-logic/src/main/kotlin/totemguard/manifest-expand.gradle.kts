package totemguard

import totemguard.build.withoutSnapshotHash

plugins {
    java
}

tasks.named<ProcessResources>("processResources") {
    val versionString = project.version.toString().withoutSnapshotHash()
    val descriptionString = project.description ?: rootProject.description ?: ""
    inputs.property("version", versionString)
    inputs.property("description", descriptionString)
    filesMatching(listOf("plugin.yml", "fabric.mod.json", "bungee.yml", "velocity-plugin.json")) {
        expand(mapOf("version" to versionString, "description" to descriptionString))
    }
}
