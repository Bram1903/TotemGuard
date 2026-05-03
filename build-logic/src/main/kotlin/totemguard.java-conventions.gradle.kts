plugins {
    java
}

version = rootProject.version

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 17
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.named<ProcessResources>("processResources").configure {
    val versionWithoutHash = provider { project.version.toString().withoutSnapshotHash() }
    inputs.property("version", versionWithoutHash)

    filesMatching(listOf("plugin.yml", "fabric.mod.json")) {
        expand("version" to versionWithoutHash.get())
    }
}

fun String.withoutSnapshotHash(): String {
    return replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
}
