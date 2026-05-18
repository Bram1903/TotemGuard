import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("totemguard.shadow-conventions")
    alias(libs.plugins.fabric.loom)
}

version = rootProject.version
group = rootProject.group

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    implementation(project(":common"))
    // Paper bundles snakeyaml at runtime, Fabric does not, so the Fabric plugin
    // must ship its own copy. common/ keeps it compileOnly so Paper isn't double-bundled.
    implementation(libs.snakeyaml)
    implementation(libs.mysql.jdbc) {
        exclude(group = "org.slf4j")
        exclude(group = "com.google.protobuf")
    }

    implementation(libs.adventure.platform.fabric)
    implementation(libs.cloud.fabric)
    include(libs.cloud.fabric)
    implementation(libs.fabric.permissions.api)
    include(libs.fabric.permissions.api)

    compileOnly("org.jetbrains:annotations:26.0.2-1")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

java {
    // cloud-fabric 2.0.0-beta.16 ships Java 25 bytecode, so a JDK 25 toolchain
    // is required to read its class files at compile time.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 25
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.named<ProcessResources>("processResources").configure {
    val versionWithoutHash = provider {
        project.version.toString().withoutSnapshotHash()
    }
    val description = provider { rootProject.description ?: "" }
    inputs.property("version", versionWithoutHash)
    inputs.property("description", description)
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to versionWithoutHash.get(),
                "description" to description.get()
            )
        )
    }
}

tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier = "dev"
    destinationDirectory = layout.buildDirectory.dir("libs")
}

tasks.named<ShadowJar>("shadowJar") {
    val devJar = tasks.named<Jar>("jar")
    dependsOn(devJar)
    from(devJar.flatMap { it.archiveFile }.map { zipTree(it) })

    relocate("com.mysql", "com.deathmotion.totemguard.common.libs.mysql")
    relocate("org.yaml.snakeyaml", "com.deathmotion.totemguard.common.libs.snakeyaml")

    dependencies {
        include(project(":api"))
        include(project(":common"))
        include(project(":integrity"))
        include(project(":bridge:protocol"))
        // Loader-host types are extracted from the plugin jar by ApiClassInjector
        // and injected into the loader's classloader, so they have to ship in the jar.
        include(project(":loader:host"))
        include(dependency("io.lettuce:.*"))
        include(dependency("io.netty:.*"))
        include(dependency("io.projectreactor:.*"))
        include(dependency("org.reactivestreams:.*"))
        include(dependency("com.zaxxer:HikariCP:.*"))
        include(dependency("com.mysql:.*"))
        include(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
        include(dependency("com.google.errorprone:.*"))
        include(dependency("org.jspecify:.*"))
        include(dependency("org.yaml:snakeyaml:.*"))
    }
}

fun String.withoutSnapshotHash(): String =
    replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
