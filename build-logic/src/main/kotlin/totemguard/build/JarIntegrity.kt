/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package totemguard.build

import java.io.BufferedOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private const val INTEGRITY_ENTRY = "META-INF/totemguard/integrity.sha256"
private val HEX: HexFormat = HexFormat.of()

fun writeJarIntegrity(jarPath: Path) {
    val fingerprint = computeJarIntegrityFingerprint(jarPath)
    val tempFile = Files.createTempFile(jarPath.parent, jarPath.fileName.toString(), ".tmp")

    try {
        ZipFile(jarPath.toFile()).use { input ->
            ZipOutputStream(BufferedOutputStream(Files.newOutputStream(tempFile))).use { output ->
                val entries = input.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name == INTEGRITY_ENTRY) continue

                    output.putNextEntry(entry.copyForOutput())
                    if (!entry.isDirectory) {
                        input.getInputStream(entry).use { stream -> stream.copyTo(output) }
                    }
                    output.closeEntry()
                }

                output.putNextEntry(ZipEntry(INTEGRITY_ENTRY))
                output.write((fingerprint + "\n").toByteArray(StandardCharsets.UTF_8))
                output.closeEntry()
            }
        }

        Files.move(tempFile, jarPath, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(tempFile)
    }
}

private fun computeJarIntegrityFingerprint(jarPath: Path): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    ZipFile(jarPath.toFile()).use { zipFile ->
        zipFile.stream()
            .filter { !it.isDirectory }
            .filter { it.name != INTEGRITY_ENTRY }
            .sorted(compareBy(ZipEntry::getName))
            .forEach { entry ->
                val nameBytes = entry.name.toByteArray(StandardCharsets.UTF_8)
                digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(nameBytes.size).array())
                digest.update(nameBytes)
                digest.update(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(entry.size).array())

                zipFile.getInputStream(entry).use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        digest.update(buffer, 0, read)
                    }
                }
            }
    }

    return HEX.formatHex(digest.digest())
}

private fun ZipEntry.copyForOutput(): ZipEntry {
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
