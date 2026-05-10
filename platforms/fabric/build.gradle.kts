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

// shadow-conventions disables `jar`. Loom hooks into `jar` to attach JIJ
// entries (the META-INF/jars/ payload from `include(...)`), so re-enable it
// and produce a "dev"-classified intermediate. shadowJar then consumes that
// jar's content so the final fat jar carries both the JIJ payload (cloud-
// fabric, fabric-permissions-api) and the relocated, shaded common-side libs.
tasks.named<Jar>("jar") {
    enabled = true
    archiveClassifier = "dev"
    destinationDirectory = layout.buildDirectory.dir("libs")
}

tasks.named<ShadowJar>("shadowJar") {
    val devJar = tasks.named<Jar>("jar")
    dependsOn(devJar)
    // Pull in the JIJ-bearing dev jar (META-INF/jars/, fabric.mod.json with
    // version expanded). Project classes are already on shadowJar's input via
    // the runtime classpath; duplicates are dropped automatically.
    from(devJar.flatMap { it.archiveFile }.map { zipTree(it) })

    // Fabric only shades MySQL — cloud (cloud-fabric mod, JIJ'd), adventure
    // (adventure-platform-fabric mod, JIJ'd), and bstats (no Fabric variant)
    // are NOT bundled here, so relocating their packages would just rewrite
    // :common's references to non-existent paths.
    relocate("com.mysql", "com.deathmotion.totemguard.common.libs.mysql")

    // Whitelist what gets shaded. Bukkit/Velocity/Bungee/Sponge land at ~7 MB
    // because their server API is `compileOnly`; on Fabric, loader / api / MC
    // have to be on the runtime classpath for dev mode to work, which would
    // drag the whole game (~75 MB) into shadowJar. So we explicitly include
    // only what `:common`'s runtime needs — everything else is supplied by
    // the runtime or JIJ'd as a Fabric mod.
    dependencies {
        include(project(":api"))
        include(project(":common"))
        include(dependency("io.lettuce:.*"))
        include(dependency("io.netty:.*"))
        include(dependency("io.projectreactor:.*"))
        include(dependency("org.reactivestreams:.*"))
        include(dependency("com.zaxxer:HikariCP:.*"))
        include(dependency("com.mysql:.*"))
        // Kept for minimize() so its used classes survive trimming.
        include(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
        include(dependency("com.google.errorprone:.*"))
        include(dependency("org.jspecify:.*"))
    }
}

fun String.withoutSnapshotHash(): String =
    replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
