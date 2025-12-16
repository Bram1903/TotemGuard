import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
            relocate("org.incendo.cloud", "com.deathmotion.totemguard.common.libs.cloud")
            relocate("io.leangen.geantyref", "com.deathmotion.totemguard.common.libs.geantyref")
            relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")
        }
    }
}
