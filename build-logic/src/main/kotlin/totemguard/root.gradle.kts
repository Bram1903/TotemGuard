package totemguard

import totemguard.build.GitHashValueSource
import totemguard.build.gitShortHash
import totemguard.build.withSnapshotMetadata

plugins {
    base
}

group = "com.deathmotion"

val baseVersion = "3.0.0"
val snapshot = true

val gitHash: Provider<String> = providers.of(GitHashValueSource::class) {
    parameters.projectDir.set(rootProject.projectDir)
}

val fullHash = gitHash.orNull
val shortHash = fullHash?.gitShortHash()

extra["snapshot"] = snapshot
extra["gitHash"] = fullHash
extra["gitHashShort"] = shortHash

version = baseVersion.withSnapshotMetadata(snapshot, shortHash)
description = "TotemGuard is a simple anti-cheat that tries to detect players who are using AutoTotem."
