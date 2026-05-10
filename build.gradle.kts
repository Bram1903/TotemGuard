group = "com.deathmotion"

val baseVersion = "3.0.0"
val snapshot = true
val gitHash: String? = resolveGitHash()

extra["snapshot"] = snapshot
extra["gitHash"] = gitHash

version = baseVersion.withSnapshotMetadata(snapshot, gitHash)
description = "TotemGuard is a simple anti-cheat that tries to detect players who are using AutoTotem."

subprojects {
    group = rootProject.group
    if (version.toString() == "unspecified") {
        version = rootProject.version
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

fun String.withSnapshotMetadata(snapshot: Boolean, gitHash: String?): String {
    if (!snapshot) {
        return this
    }

    return buildString {
        append(this@withSnapshotMetadata)
        if (!gitHash.isNullOrBlank()) {
            append("+")
            append(gitHash)
        }
        append("-SNAPSHOT")
    }
}

fun Project.resolveGitHash(): String? {
    if (!file(".git").exists()) {
        return null
    }

    return runCatching {
        providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim().ifBlank { null }
    }.getOrNull()
}
