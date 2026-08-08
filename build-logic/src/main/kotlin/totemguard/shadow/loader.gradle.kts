package totemguard.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import totemguard.build.withoutSnapshotHash

plugins {
    id("totemguard.shadow.conventions")
}

tasks.withType<ShadowJar>().configureEach {
    archiveFileName.set("${rootProject.name}-Loader-${project.version.toString().withoutSnapshotHash()}.jar")

    relocate(
        "com.deathmotion.totemguard.integrity",
        "com.deathmotion.totemguard.loader.libs.integrity"
    )

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
        attributes["Implementation-Version"] = project.version.toString()
    }
}
