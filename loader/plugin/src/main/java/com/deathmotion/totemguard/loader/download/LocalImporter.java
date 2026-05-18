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

package com.deathmotion.totemguard.loader.download;

import com.deathmotion.totemguard.integrity.JarIntegrityChecker;
import com.deathmotion.totemguard.loader.catalog.CatalogIndex;
import com.deathmotion.totemguard.loader.catalog.CatalogSidecar;
import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.core.LoaderPaths;
import com.deathmotion.totemguard.loader.core.PluginVersionGate;
import com.deathmotion.totemguard.loader.source.Artifact;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

public final class LocalImporter {

    private static final String IMPORTED_DIR = ".imported";

    private final Logger logger;

    public LocalImporter(Logger logger) {
        this.logger = logger;
    }

    private static String readJarVersion(Path jar) throws IOException {
        try (JarFile jf = new JarFile(jar.toFile())) {
            String fromPaper = readPaperVersion(jf);
            if (fromPaper != null) return fromPaper;
            return readFabricVersion(jf);
        }
    }

    private static String readPaperVersion(JarFile jf) throws IOException {
        ZipEntry entry = jf.getEntry("plugin.yml");
        if (entry == null) return null;
        try (InputStream in = jf.getInputStream(entry)) {
            for (String line : new String(in.readAllBytes()).split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("version:")) {
                    String value = trimmed.substring("version:".length()).trim();
                    value = value.replace("\"", "").replace("'", "");
                    return value.isEmpty() ? null : value;
                }
            }
        }
        return null;
    }

    private static String readFabricVersion(JarFile jf) throws IOException {
        ZipEntry entry = jf.getEntry("fabric.mod.json");
        if (entry == null) return null;
        try (InputStream in = jf.getInputStream(entry)) {
            JsonObject root = JsonParser.parseString(new String(in.readAllBytes())).getAsJsonObject();
            if (!root.has("version")) return null;
            return root.get("version").getAsString();
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._+-]", "_");
    }

    public int importAll(LoaderPaths paths, HostPlatform platform) {
        return importAll(paths, platform, null);
    }

    public int importAll(LoaderPaths paths, HostPlatform platform, @Nullable Consumer<ImportRecord> onImported) {
        Path localDir = paths.localDir();
        if (!Files.isDirectory(localDir)) return 0;

        Path importedDir = localDir.resolve(IMPORTED_DIR);
        int imported = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(localDir, "*.jar")) {
            for (Path jar : stream) {
                if (!Files.isRegularFile(jar)) continue;
                try {
                    ImportRecord record = importOne(jar, paths, platform);
                    if (record != null) {
                        Files.createDirectories(importedDir);
                        Files.move(jar, importedDir.resolve(jar.getFileName()),
                                StandardCopyOption.REPLACE_EXISTING);
                        imported++;
                        if (onImported != null && record.freshlyImported()) {
                            try {
                                onImported.accept(record);
                            } catch (Throwable t) {
                                logger.log(Level.WARNING, "onImported callback threw", t);
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Failed to import " + jar.getFileName(), ex);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to scan local import directory " + localDir, ex);
        }
        return imported;
    }

    private ImportRecord importOne(Path jar, LoaderPaths paths, HostPlatform platform) throws IOException {
        if (!new JarIntegrityChecker(logger, "TotemGuard").verifyJar(jar)) {
            logger.warning("Skipping " + jar.getFileName() + ": jar integrity check failed.");
            return null;
        }

        String version = readJarVersion(jar);
        if (version == null) {
            logger.warning("Skipping " + jar.getFileName() + ": couldn't read plugin.yml / fabric.mod.json version.");
            return null;
        }
        if (!PluginVersionGate.isSupportedConcrete(version)) {
            logger.warning("Skipping " + jar.getFileName() + ": version " + version
                    + " is older than " + PluginVersionGate.MINIMUM + ".");
            return null;
        }

        String sha256 = Checksums.hashFile(jar, Artifact.HashAlgorithm.SHA_256);
        String hashPrefix = sha256.substring(0, Math.min(10, sha256.length()));
        Path destination = paths.versionsDir().resolve("TotemGuard-"
                + platform.name().toLowerCase(Locale.ROOT)
                + "-" + sanitize(version)
                + "-" + hashPrefix + ".jar");

        CatalogIndex index = new CatalogIndex(paths.versionsDir(), platform, logger);
        boolean freshlyImported;
        if (Files.isRegularFile(destination)) {
            logger.info("Local " + jar.getFileName() + " already in cache as "
                    + destination.getFileName() + ".");
            index.upsertSource(destination, version, sha256, CatalogSidecar.SOURCE_LOCAL, null);
            freshlyImported = false;
        } else {
            Files.createDirectories(destination.getParent());
            Files.copy(jar, destination, StandardCopyOption.REPLACE_EXISTING);
            index.upsertSource(destination, version, sha256, CatalogSidecar.SOURCE_LOCAL, null);
            logger.info("Imported " + jar.getFileName() + " as " + destination.getFileName() + ".");
            freshlyImported = true;
        }
        return new ImportRecord(destination, version, sha256, freshlyImported);
    }

    public record ImportRecord(Path jar, String version, String sha256, boolean freshlyImported) {
    }
}
