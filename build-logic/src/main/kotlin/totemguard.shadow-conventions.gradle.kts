import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import java.io.BufferedOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private val JAR_INTEGRITY_ENTRY = "META-INF/totemguard/integrity.sha256"
private val JAR_INTEGRITY_HEX: HexFormat = HexFormat.of()

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

    relocate("org.incendo.cloud", "com.deathmotion.totemguard.common.libs.cloud")
    relocate("io.leangen.geantyref", "com.deathmotion.totemguard.common.libs.geantyref")
    relocate("org.bstats", "com.deathmotion.totemguard.common.libs.bstats")
    relocate("io.lettuce", "com.deathmotion.totemguard.common.libs.lettuce")
    relocate("io.netty", "com.deathmotion.totemguard.common.libs.netty")
    relocate("org.reactivestreams", "com.deathmotion.totemguard.common.libs.reactivestreams")
    relocate("reactor", "com.deathmotion.totemguard.common.libs.reactor")
    relocate("redis.clients", "com.deathmotion.totemguard.common.libs.redisclients")
    relocate("com.zaxxer.hikari", "com.deathmotion.totemguard.common.libs.hikari")
    relocate("com.google.errorprone.annotations", "com.deathmotion.totemguard.common.libs.errorprone.annotations")
    relocate("org.jspecify.annotations", "com.deathmotion.totemguard.common.libs.jspecify.annotations")

    mergeServiceFiles()

    minimize {
        exclude(dependency("com.github.ben-manes.caffeine:caffeine:.*"))
        exclude(dependency("org.bstats:.*:.*"))
    }

    doLast {
        writeJarIntegrity(archiveFile.get().asFile.toPath())
    }
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}

fun String.withoutSnapshotHash(): String {
    return replace(Regex("\\+[0-9a-f]+-SNAPSHOT$"), "-SNAPSHOT")
}

fun String.capitalizedName(): String {
    return replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

fun writeJarIntegrity(jarPath: java.nio.file.Path) {
    val fingerprint = computeJarIntegrityFingerprint(jarPath)
    val tempFile = Files.createTempFile(jarPath.parent, jarPath.fileName.toString(), ".tmp")

    try {
        ZipFile(jarPath.toFile()).use { input ->
            ZipOutputStream(BufferedOutputStream(Files.newOutputStream(tempFile))).use { output ->
                val entries = input.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name == JAR_INTEGRITY_ENTRY) {
                        continue
                    }

                    output.putNextEntry(entry.copyForOutput())
                    if (!entry.isDirectory) {
                        input.getInputStream(entry).use { stream -> stream.copyTo(output) }
                    }
                    output.closeEntry()
                }

                output.putNextEntry(ZipEntry(JAR_INTEGRITY_ENTRY))
                output.write((fingerprint + "\n").toByteArray(StandardCharsets.UTF_8))
                output.closeEntry()
            }
        }

        Files.move(tempFile, jarPath, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(tempFile)
    }
}

fun computeJarIntegrityFingerprint(jarPath: java.nio.file.Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    ZipFile(jarPath.toFile()).use { zipFile ->
        zipFile.stream()
            .filter { !it.isDirectory }
            .filter { it.name != JAR_INTEGRITY_ENTRY }
            .sorted(compareBy(ZipEntry::getName))
            .forEach { entry ->
                val nameBytes = entry.name.toByteArray(StandardCharsets.UTF_8)
                digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(nameBytes.size).array())
                digest.update(nameBytes)
                digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(entry.size).array())

                zipFile.getInputStream(entry).use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) {
                            break
                        }
                        digest.update(buffer, 0, read)
                    }
                }
            }
    }

    return JAR_INTEGRITY_HEX.formatHex(digest.digest())
}

fun ZipEntry.copyForOutput(): ZipEntry {
    return ZipEntry(name).apply {
        comment = this@copyForOutput.comment
        extra = this@copyForOutput.extra
        method = this@copyForOutput.method
        this@copyForOutput.creationTime?.let { creationTime = it }
        this@copyForOutput.lastAccessTime?.let { lastAccessTime = it }
        this@copyForOutput.lastModifiedTime?.let { lastModifiedTime = it }

        if (method == ZipEntry.STORED) {
            size = this@copyForOutput.size
            compressedSize = this@copyForOutput.compressedSize
            crc = this@copyForOutput.crc
        }
    }
}
