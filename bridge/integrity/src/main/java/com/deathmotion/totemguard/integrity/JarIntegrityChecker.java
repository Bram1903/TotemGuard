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

package com.deathmotion.totemguard.integrity;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Verifies that the jar this class is loaded from matches the SHA-256 fingerprint stamped
 * at build time under {@code META-INF/totemguard/integrity.sha256}. Re-used by both the
 * TotemGuard backend and the TotemGuard-Bridge plugin via the {@code :bridge:integrity}
 * module.
 *
 * <p>Stateless except for the {@link Logger} and a {@code productName} (used purely for the
 * tamper-alert banner). Returns {@code true} when the jar matches or when running from a
 * development classpath (no jar to verify).</p>
 */
public final class JarIntegrityChecker {

    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String INTEGRITY_ENTRY = "META-INF/totemguard/integrity.sha256";
    private static final String SEPARATOR =
            "======================================================================";

    private final Logger logger;
    private final String productName;

    public JarIntegrityChecker(@NotNull Logger logger, @NotNull String productName) {
        this.logger = logger;
        this.productName = productName;
    }

    public boolean verifyCurrentJar() {
        logger.info("Checking jar integrity...");

        Path jarPath = resolveCurrentJarPath();
        if (jarPath == null) {
            return true;
        }

        String expectedFingerprint = readExpectedFingerprint();
        if (expectedFingerprint == null || expectedFingerprint.isBlank()) {
            logSuspiciousJar(
                    productName + " could not verify its embedded jar metadata.",
                    "This jar does not look like an original " + productName + " build.",
                    "The jar is missing its embedded integrity metadata.",
                    "This usually means the jar was rebuilt, unpacked, or modified."
            );
            return false;
        }

        try {
            String actualFingerprint = computeFingerprint(jarPath);
            if (actualFingerprint.equalsIgnoreCase(expectedFingerprint)) {
                logger.info("Successfully validated jar integrity.");
                return true;
            }

            logSuspiciousJar(
                    productName + " detected that the jar was modified since build.",
                    "This is most likely malware or direct jar tampering."
            );
            return false;
        } catch (IOException | NoSuchAlgorithmException exception) {
            logSuspiciousJar(
                    productName + " could not complete its jar integrity check.",
                    "This usually means the jar is damaged, modified, or being interfered with.",
                    "Verification error: " + exception.getClass().getSimpleName()
            );
            return false;
        }
    }

    private Path resolveCurrentJarPath() {
        CodeSource codeSource = JarIntegrityChecker.class.getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return null;
        }

        try {
            Path path = Path.of(codeSource.getLocation().toURI()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path) || !path.getFileName().toString().endsWith(".jar")) {
                // Development classpath (classes on disk, not a jar) — nothing to verify.
                return null;
            }
            return path;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private String readExpectedFingerprint() {
        try (InputStream inputStream = JarIntegrityChecker.class.getClassLoader().getResourceAsStream(INTEGRITY_ENTRY)) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            return null;
        }
    }

    private String computeFingerprint(Path jarPath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] buffer = new byte[8192];

        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            List<? extends ZipEntry> entries = zipFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> !Objects.equals(entry.getName(), INTEGRITY_ENTRY))
                    .sorted(Comparator.comparing(ZipEntry::getName))
                    .toList();

            for (ZipEntry entry : entries) {
                byte[] nameBytes = entry.getName().getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(nameBytes.length).array());
                digest.update(nameBytes);
                digest.update(ByteBuffer.allocate(Long.BYTES).putLong(entry.getSize()).array());

                try (InputStream inputStream = zipFile.getInputStream(entry)) {
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private void logSuspiciousJar(String headline, String assessment, String... details) {
        logger.severe(SEPARATOR);
        logger.severe(" " + productName.toUpperCase() + " SECURITY ALERT ");
        logger.severe(SEPARATOR);
        logger.severe("");
        logger.severe(" " + headline);
        logger.severe(" " + assessment);

        for (String detail : details) {
            logger.severe(" " + detail);
        }

        logger.severe("");
        logger.severe(" Required action:");
        logger.severe(" 1. Delete this " + productName + " jar.");
        logger.severe(" 2. Reinstall " + productName + " from a trusted source.");
        logger.severe(" 3. If this warning appears again after reinstalling");
        logger.severe("    " + productName + ", malware is most likely modifying");
        logger.severe("    plugin jars or the server jar during startup.");
        logger.severe(" 4. Reinstall your server jar and every plugin");
        logger.severe("    from trusted sources.");
        logger.severe("");
        logger.severe(" " + productName + " has disabled itself.");
        logger.severe(SEPARATOR);
    }
}
