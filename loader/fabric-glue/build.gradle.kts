import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    java
    alias(libs.plugins.fabric.loom)
}

// Same version literal as loader/plugin so fabric.mod.json reports the loader
// version, not the rootProject TotemGuard plugin version. Kept in sync by hand.
version = "1.0.0" + if (rootProject.extra["snapshot"] as Boolean) "-SNAPSHOT" else ""
group = rootProject.group
description = "TotemGuard Loader Fabric glue (CommandRegistrationCallback + CommandSourceStack hooks)"

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    // SLF4J is already on Fabric's runtime classpath (Fabric Loader uses it).
    // compileOnly so we don't ship our own copy.
    compileOnly("org.slf4j:slf4j-api:2.0.13")

    // implementation (not compileOnly) so loom's source set transformation actually
    // puts these on the fabric-glue compile classpath. The shaded loader jar already
    // ships these classes, so we don't ship them again here. The loader-shadow
    // configuration handles the merge.
    implementation(project(":api"))
    implementation(project(":loader:host"))
    implementation(project(":loader:plugin"))
    implementation(project(":integrity"))

    // Puts cloud on Knot in dev. Production JIJ is packed by loader/plugin shadowJar.
    implementation(libs.cloud.fabric)
    implementation(libs.adventure.platform.fabric)
    implementation(libs.fabric.permissions.api)
}

java {
    // fabric-api 0.145+ ships Java 25 bytecode, so this module needs the JDK 25
    // toolchain to compile. Release is held at 17 so the remapped output drops into
    // the loader jar without forcing the Paper runtime to a newer JVM.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    disableAutoTargetJvm()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 17
}

tasks.withType<Test>().configureEach {
    failOnNoDiscoveredTests = false
}

tasks.named("runServer").configure {
    val fabricShadow = project(":platforms:fabric").tasks.named("shadowJar")
    dependsOn(fabricShadow)
    doFirst {
        val localDir = layout.projectDirectory.dir("run/config/totemguard-loader/local").asFile.toPath()
        Files.createDirectories(localDir)
        Files.newDirectoryStream(localDir, "*.jar").use { entries ->
            entries.forEach { Files.delete(it) }
        }
        val source = fabricShadow.get().outputs.files.singleFile.toPath()
        val destination = localDir.resolve(source.fileName.toString())
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
        logger.lifecycle("Copied ${source.fileName} -> $destination for /tgloader LOCAL source.")
    }
}

tasks.named<ProcessResources>("processResources").configure {
    val versionWithoutHash = provider {
        project.version.toString().replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
    }
    val description = provider { rootProject.description ?: "" }
    inputs.property("version", versionWithoutHash)
    inputs.property("description", description)
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to versionWithoutHash.get(),
                "description" to description.get(),
            )
        )
    }
}
