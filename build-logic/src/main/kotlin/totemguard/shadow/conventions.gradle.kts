package totemguard.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import totemguard.build.capitalizedName
import totemguard.build.withoutSnapshotHash
import totemguard.build.writeJarIntegrity

plugins {
    id("com.gradleup.shadow")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<ShadowJar>().configureEach {
    archiveFileName.set(
        "${rootProject.name}-${project.name.capitalizedName()}-${project.version.toString().withoutSnapshotHash()}.jar"
    )
    archiveClassifier.set(null as String?)
    destinationDirectory.set(rootProject.layout.buildDirectory)

    exclude("META-INF/maven/**")
    exclude("INFO_BIN", "INFO_SRC", "LICENSE", "README", "LICENSE.txt", "README.txt")

    mergeServiceFiles()

    val outputFile = archiveFile
    doLast {
        writeJarIntegrity(outputFile.get().asFile.toPath())
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
