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
import java.util.Properties;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;

public final class LocalImporter {

    private static final String IMPORTED_DIR = ".imported";
    private static final String BUILD_PROPS_ENTRY = "META-INF/totemguard/build.properties";

    private final Logger logger;
    private final CatalogIndex catalogIndex;

    public LocalImporter(Logger logger, CatalogIndex catalogIndex) {
        this.logger = logger;
        this.catalogIndex = catalogIndex;
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

    private static BuildProps readBuildProps(Path jar) {
        try (JarFile jf = new JarFile(jar.toFile())) {
            ZipEntry entry = jf.getEntry(BUILD_PROPS_ENTRY);
            if (entry == null) return BuildProps.EMPTY;
            Properties props = new Properties();
            try (InputStream in = jf.getInputStream(entry)) {
                props.load(in);
            }
            String commit = props.getProperty("git.commit", "").trim();
            long timestamp = 0L;
            String tsRaw = props.getProperty("build.timestamp", "").trim();
            if (!tsRaw.isEmpty()) {
                try {
                    timestamp = Long.parseLong(tsRaw);
                } catch (NumberFormatException ignored) {
                }
            }
            return new BuildProps(commit, timestamp);
        } catch (IOException ex) {
            return BuildProps.EMPTY;
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
        BuildProps incomingProps = readBuildProps(jar);
        if (incomingProps.gitCommit().isEmpty()) {
            logger.warning("Skipping " + jar.getFileName()
                    + ": missing META-INF/totemguard/build.properties (was this built from TotemGuard 3.0 or newer?).");
            return null;
        }

        Path destination = paths.versionsDir().resolve("TotemGuard-"
                + platform.name().toLowerCase(Locale.ROOT)
                + "-" + sanitize(version)
                + "-" + sanitize(incomingProps.gitCommit()) + ".jar");

        boolean freshlyImported;
        if (Files.isRegularFile(destination)) {
            String existingSha = Checksums.hashFile(destination, Artifact.HashAlgorithm.SHA_256);
            if (existingSha.equalsIgnoreCase(sha256)) {
                logger.info("Local " + jar.getFileName() + " already in cache as "
                        + destination.getFileName() + ".");
                catalogIndex.upsertSource(destination, version, sha256, CatalogSidecar.SOURCE_LOCAL, null);
                freshlyImported = false;
            } else {
                // Same commit (or same SHA-prefix), different content. Newer build wins.
                BuildProps existingProps = readBuildProps(destination);
                if (incomingProps.buildTimestamp() <= existingProps.buildTimestamp()
                        && existingProps.buildTimestamp() > 0L) {
                    logger.warning("Skipping " + jar.getFileName() + ": cached "
                            + destination.getFileName() + " was built at "
                            + existingProps.buildTimestamp() + " and the incoming jar is older.");
                    return null;
                }
                Files.copy(jar, destination, StandardCopyOption.REPLACE_EXISTING);
                catalogIndex.upsertSource(destination, version, sha256, CatalogSidecar.SOURCE_LOCAL, null);
                logger.info("Replaced " + destination.getFileName()
                        + " with a newer build of the same commit.");
                freshlyImported = true;
            }
        } else {
            Files.createDirectories(destination.getParent());
            Files.copy(jar, destination, StandardCopyOption.REPLACE_EXISTING);
            catalogIndex.upsertSource(destination, version, sha256, CatalogSidecar.SOURCE_LOCAL, null);
            logger.info("Imported " + jar.getFileName() + " as " + destination.getFileName() + ".");
            freshlyImported = true;
        }
        return new ImportRecord(destination, version, sha256, freshlyImported);
    }

    private record BuildProps(String gitCommit, long buildTimestamp) {
        static final BuildProps EMPTY = new BuildProps("", 0L);
    }

    public record ImportRecord(Path jar, String version, String sha256, boolean freshlyImported) {
    }
}
