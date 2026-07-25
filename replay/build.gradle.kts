plugins {
    id("totemguard.java.internal")
}

description = "Headless replay runner. Never shipped, never shaded."

dependencies {
    implementation(projects.common)

    implementation(libs.packetevents.api)
    implementation(libs.packetevents.netty.common)
    implementation(libs.bundles.adventure)
    implementation(libs.adventure.nbt)
    implementation(libs.bundles.adventure.serializers)
    implementation(libs.bundles.adventure.minimessage)
    implementation(libs.gson)
    implementation(libs.snakeyaml)
    implementation(libs.guava)
    implementation(libs.cloud.core)
}

tasks.register<JavaExec>("replay") {
    group = "verification"
    description = "Replays the recording library through the real engine and reports what changed."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.deathmotion.totemguard.replay.ReplayMain"
    workingDir = layout.projectDirectory.asFile

    val recordings = providers.gradleProperty("recordings").orNull
        ?.let { layout.projectDirectory.dir(it).asFile.absolutePath }
        ?: layout.projectDirectory.dir("recordings").asFile.absolutePath
    val only = providers.gradleProperty("only").orNull
    val trace = providers.gradleProperty("trace").orNull
    val from = providers.gradleProperty("from").orNull
    val to = providers.gradleProperty("to").orNull
    val accept = providers.gradleProperty("accept").isPresent
    val diff = providers.gradleProperty("diff").isPresent
    val determinism = providers.gradleProperty("verify-determinism").isPresent
    val selfTest = providers.gradleProperty("selftest").isPresent
    val analyze = providers.gradleProperty("analyze").isPresent
    val recordedConfig = providers.gradleProperty("config").orNull == "recorded"

    args = buildList {
        add("--recordings=$recordings")
        if (only != null) add("--only=$only")
        if (trace != null) add("--trace=$trace")
        if (from != null) add("--from=$from")
        if (to != null) add("--to=$to")
        if (accept) add("--accept")
        if (diff) add("--diff")
        if (determinism) add("--verify-determinism")
        if (recordedConfig) add("--config=recorded")
        if (selfTest) add("--selftest")
        if (analyze) add("--analyze")
    }
}