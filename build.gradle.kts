import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Locale.getDefault

plugins {
    alias(libs.plugins.shadow)
}

val fullVersion = "3.0.0"
val snapshot = true

fun getVersionMeta(includeHash: Boolean): String {
    if (!snapshot) {
        return ""
    }

    var commitHash = ""
    if (includeHash && file(".git").isDirectory) {
        val result = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()

        commitHash = "+$result"
    }

    return "$commitHash-SNAPSHOT"
}
version = "$fullVersion${getVersionMeta(true)}"
ext["versionNoHash"] = "$fullVersion${getVersionMeta(false)}"

subprojects {
    plugins.withId("com.github.johnrengelman.shadow") {
        tasks.withType<ShadowJar>().configureEach {
            archiveFileName = "${rootProject.name}-${
                project.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        getDefault()
                    ) else it.toString()
                }
            }-${rootProject.ext["versionNoHash"]}.jar"
            archiveClassifier = null
            destinationDirectory = rootProject.layout.buildDirectory
            exclude("META-INF/maven/**")

            // Cloud
            relocate("org.incendo.cloud", "com.deathmotion.totemguard.common.libs.cloud")
            relocate("io.leangen.geantyref", "com.deathmotion.totemguard.common.libs.geantyref")

            // BStats
            relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")

            // Lettuce
            relocate("io.lettuce", "com.deathmotion.totemguard.common.libs.lettuce")
            relocate("io.netty", "com.deathmotion.totemguard.common.libs.netty")
            relocate("org.reactivestreams", "com.deathmotion.totemguard.common.libs.reactivestreams")
            relocate("org.slf4j", "com.deathmotion.totemguard.common.libs.slf4j")
            relocate("reactor", "com.deathmotion.totemguard.common.libs.reactor")
            relocate("redis.clients", "com.deathmotion.totemguard.common.libs.redisclients")

            minimize()
        }
    }
}

tasks {
    register<Delete>("clean") {
        delete(rootProject.layout.buildDirectory)
    }
}
