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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(jarPath));
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        }
    }

    private static void discard(LoaderPaths paths) {
        try {
            Files.deleteIfExists(paths.stagedJar());
            Files.deleteIfExists(paths.stagedMeta());
        } catch (IOException ignored) {
        }
    }
}
