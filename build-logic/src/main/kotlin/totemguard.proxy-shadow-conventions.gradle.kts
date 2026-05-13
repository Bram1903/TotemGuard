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
    archiveFileName =
        "${rootProject.name}-${project.name.capitalizedName()}-${project.version.toString().withoutSnapshotHash()}.jar"
    archiveClassifier = null
    destinationDirectory = rootProject.layout.buildDirectory
    exclude("META-INF/maven/**")
    exclude("INFO_BIN", "INFO_SRC", "LICENSE", "README", "LICENSE.txt", "README.txt")

    val libsPrefix = "com.deathmotion.totemguard.proxybridge.libs"
    relocate("io.lettuce", "$libsPrefix.lettuce")
    relocate("io.netty", "$libsPrefix.netty")
    relocate("org.reactivestreams", "$libsPrefix.reactivestreams")
    relocate("reactor", "$libsPrefix.reactor")

    mergeServiceFiles()
    minimize()

    doLast {
        writeJarIntegrity(archiveFile.get().asFile.toPath())
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
