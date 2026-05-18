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

    relocate("io.lettuce", "com.deathmotion.totemguard.common.libs.lettuce")
    relocate("io.netty", "com.deathmotion.totemguard.common.libs.netty")
    relocate("org.reactivestreams", "com.deathmotion.totemguard.common.libs.reactivestreams")
    relocate("reactor", "com.deathmotion.totemguard.common.libs.reactor")
    relocate("redis.clients", "com.deathmotion.totemguard.common.libs.redisclients")
    relocate("com.zaxxer.hikari", "com.deathmotion.totemguard.common.libs.hikari")
    relocate("com.google.errorprone.annotations", "com.deathmotion.totemguard.common.libs.errorprone.annotations")
    relocate("org.jspecify.annotations", "com.deathmotion.totemguard.common.libs.jspecify.annotations")
    relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")
    relocate("org.incendo.cloud", "com.deathmotion.totemguard.common.libs.cloud")
    relocate("io.leangen.geantyref", "com.deathmotion.totemguard.common.libs.geantyref")

    mergeServiceFiles()

    minimize {
        exclude(dependency("org.bstats:.*:.*"))
        exclude(dependency("com.mysql:.*"))
        // Loader-host classes are picked up at runtime by ApiClassInjector (which
        // scans the plugin jar for com/deathmotion/totemguard/host/ entries), so
        // they must survive even though plugin code doesn't reference them all.
        exclude(project(":loader:host"))
    }

    doLast {
        writeJarIntegrity(archiveFile.get().asFile.toPath())
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
