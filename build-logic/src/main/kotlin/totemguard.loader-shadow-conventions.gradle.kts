import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import totemguard.build.writeJarIntegrity

plugins {
    id("com.gradleup.shadow")
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<ShadowJar>().configureEach {
    archiveFileName = "${rootProject.name}-Loader-${project.version}.jar"
    archiveClassifier = null
    destinationDirectory = rootProject.layout.buildDirectory
    exclude("META-INF/maven/**")
    exclude("INFO_BIN", "INFO_SRC", "LICENSE", "README", "LICENSE.txt", "README.txt")

    relocate(
        "com.deathmotion.totemguard.integrity",
        "com.deathmotion.totemguard.loader.libs.integrity"
    )

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
        attributes["Implementation-Version"] = project.version.toString()
    }

    mergeServiceFiles()

    doLast {
        writeJarIntegrity(archiveFile.get().asFile.toPath())
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
