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

package com.deathmotion.totemguard.loader.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public record StagedJar(Path jar, String version, String source, String sha256) {

    public static StagedJar consumeIfPresent(LoaderPaths paths, Logger logger) {
        Path jarPath = paths.stagedJar();
        Path metaPath = paths.stagedMeta();
        if (!Files.isRegularFile(jarPath) || !Files.isRegularFile(metaPath)) {
            return null;
        }

        try {
            Map<String, String> meta = readMeta(metaPath);
            String expectedSha = meta.get("sha256");
            if (expectedSha == null) {
                logger.warning("Staged jar metadata is missing 'sha256'. Discarding.");
                discard(paths);
                return null;
            }

            String actualSha = sha256(jarPath);
            if (!actualSha.equalsIgnoreCase(expectedSha)) {
                logger.warning("Staged jar SHA-256 mismatch (expected " + expectedSha
                        + ", got " + actualSha + "). Discarding.");
                discard(paths);
                return null;
            }

            String version = meta.getOrDefault("version", "unknown");
            String source = meta.getOrDefault("source", "UNKNOWN");

            // Move the staged jar into the loader directory under a deterministic name so
            // the integrity checker sees a stable path. Then clear the marker so future
            // restarts fall back to normal resolution.
            Path active = paths.loaderDir().resolve("staged-active.jar");
            Files.move(jarPath, active, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(metaPath);

            return new StagedJar(active, version, source, actualSha);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to consume staged jar. Falling back to resolver.", ex);
            discard(paths);
            return null;
        }
    }

    public static void write(LoaderPaths paths, byte[] bytes, String version, String source, String sha256) throws IOException {
        Files.createDirectories(paths.loaderDir());
        Path tmpJar = paths.loaderDir().resolve("staged.jar.partial");
        Files.write(tmpJar, bytes);
        Files.move(tmpJar, paths.stagedJar(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        writeMeta(paths, version, source, sha256);
    }

    /**
     * Stage a jar without materialising the whole file in memory. Streams the contents
     * through a digest, copies to the staged path, and atomically moves into place.
     * The provided {@code sha256} is verified against the streamed digest to catch
     * concurrent modification of the source file between callers reading it and us
     * staging it.
     */
    public static void writeFromFile(LoaderPaths paths, Path sourceJar, String version, String source, String sha256) throws IOException {
        Files.createDirectories(paths.loaderDir());
        Path tmpJar = paths.loaderDir().resolve("staged.jar.partial");

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
        try (InputStream in = Files.newInputStream(sourceJar);
             DigestInputStream digestIn = new DigestInputStream(in, digest);
             OutputStream out = Files.newOutputStream(tmpJar,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            digestIn.transferTo(out);
        }
        String actualSha = HexFormat.of().formatHex(digest.digest());
        if (!actualSha.equalsIgnoreCase(sha256)) {
            try {
                Files.deleteIfExists(tmpJar);
            } catch (IOException ignored) {
            }
            throw new IOException("Source jar " + sourceJar.getFileName()
                    + " hash " + actualSha + " differs from expected " + sha256
                    + ". Refusing to stage.");
        }
        Files.move(tmpJar, paths.stagedJar(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        writeMeta(paths, version, source, sha256);
    }

    private static void writeMeta(LoaderPaths paths, String version, String source, String sha256) throws IOException {
        String meta = "version=" + version + "\n"
                + "source=" + source + "\n"
                + "sha256=" + sha256.toLowerCase(Locale.ROOT) + "\n";
        Path tmpMeta = paths.loaderDir().resolve("staged.meta.partial");
        Files.writeString(tmpMeta, meta, StandardCharsets.UTF_8);
        Files.move(tmpMeta, paths.stagedMeta(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Map<String, String> readMeta(Path metaPath) throws IOException {
        Map<String, String> result = new HashMap<>();
        for (String line : Files.readAllLines(metaPath, StandardCharsets.UTF_8)) {
            int equals = line.indexOf('=');
            if (equals <= 0) continue;
            result.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
        }
        return result;
    }

    private static String sha256(Path jarPath) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
        try (InputStream in = Files.newInputStream(jarPath);
             DigestInputStream digestIn = new DigestInputStream(in, digest)) {
            byte[] buffer = new byte[16_384];
            while (digestIn.read(buffer) != -1) {
                // The DigestInputStream updates the digest as a side-effect of read.
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void discard(LoaderPaths paths) {
        try {
            Files.deleteIfExists(paths.stagedJar());
            Files.deleteIfExists(paths.stagedMeta());
        } catch (IOException ignored) {
        }
    }
}
