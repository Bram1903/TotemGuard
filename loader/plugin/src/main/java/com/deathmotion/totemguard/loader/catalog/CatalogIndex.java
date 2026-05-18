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

package com.deathmotion.totemguard.loader.catalog;

import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.download.Checksums;
import com.deathmotion.totemguard.loader.source.Artifact;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CatalogIndex {

    private final Path versionsDir;
    private final HostPlatform platform;
    private final Logger logger;

    public CatalogIndex(Path versionsDir, HostPlatform platform, Logger logger) {
        this.versionsDir = versionsDir;
        this.platform = platform;
        this.logger = logger;
    }

    static String parseVersionFromFileName(String fileName) {
        // TotemGuard-<platform>-<version>-<sha10>.jar
        int firstDash = fileName.indexOf('-');
        if (firstDash < 0) return fileName;
        int secondDash = fileName.indexOf('-', firstDash + 1);
        if (secondDash < 0) return fileName;
        int lastDash = fileName.lastIndexOf('-');
        if (lastDash <= secondDash) return fileName;
        return fileName.substring(secondDash + 1, lastDash);
    }

    public List<Entry> readAll() {
        List<Entry> entries = new ArrayList<>();
        if (!Files.isDirectory(versionsDir)) return entries;
        String prefix = "TotemGuard-" + platform.name().toLowerCase(Locale.ROOT) + "-";
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsDir, prefix + "*.jar")) {
            for (Path jar : stream) {
                if (!Files.isRegularFile(jar)) continue;
                entries.add(loadEntry(jar));
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to scan catalog at " + versionsDir, ex);
        }
        return entries;
    }

    private Entry loadEntry(Path jar) {
        Path meta = CatalogSidecar.pathFor(jar);
        CatalogSidecar sidecar = null;
        if (Files.isRegularFile(meta)) {
            try {
                sidecar = CatalogSidecar.read(meta);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to read sidecar " + meta.getFileName()
                        + " (jar will be treated as unknown source)", ex);
            }
        }
        if (sidecar == null) {
            // Synthesize from filename + file mtime so legacy jars participate in retention.
            String sha = "";
            try {
                sha = Checksums.hashFile(jar, Artifact.HashAlgorithm.SHA_256);
            } catch (IOException ignored) {
            }
            Instant firstSeen;
            try {
                firstSeen = Files.getLastModifiedTime(jar).toInstant();
            } catch (IOException ignored) {
                firstSeen = Instant.EPOCH;
            }
            sidecar = CatalogSidecar.create(sha,
                    parseVersionFromFileName(jar.getFileName().toString()),
                    jar.getFileName().toString(),
                    CatalogSidecar.SOURCE_UNKNOWN,
                    null);
            // Don't write to disk; synthetic sidecars are transient.
        }
        return new Entry(jar, meta, sidecar);
    }

    public Optional<Entry> findBySha(String sha256) {
        for (Entry e : readAll()) {
            if (sha256.equalsIgnoreCase(e.sidecar().sha256())) return Optional.of(e);
        }
        return Optional.empty();
    }

    public void upsertSource(Path jar, String version, String sha256, String source, String addedBy) {
        Path meta = CatalogSidecar.pathFor(jar);
        CatalogSidecar sidecar;
        if (Files.isRegularFile(meta)) {
            try {
                sidecar = CatalogSidecar.read(meta).withAddedSource(source);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Rebuilding corrupt sidecar " + meta.getFileName(), ex);
                sidecar = CatalogSidecar.create(sha256, version, jar.getFileName().toString(), source, addedBy);
            }
        } else {
            sidecar = CatalogSidecar.create(sha256, version, jar.getFileName().toString(), source, addedBy);
        }
        try {
            sidecar.write(meta);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to write sidecar " + meta.getFileName(), ex);
        }
    }

    public void delete(Entry entry) throws IOException {
        Files.deleteIfExists(entry.jar());
        Files.deleteIfExists(entry.metaPath());
    }

    public record Entry(Path jar, Path metaPath, CatalogSidecar sidecar) {
    }
}
