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

package com.deathmotion.totemguard.common.config.service;

import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.common.config.migration.ConfigMigration;
import com.deathmotion.totemguard.common.config.migration.MigrationContext;
import com.deathmotion.totemguard.common.config.migration.MigrationRegistry;
import com.deathmotion.totemguard.common.config.yaml.DefaultsResolver;
import com.deathmotion.totemguard.common.config.yaml.YamlIO;
import com.deathmotion.totemguard.common.config.yaml.YamlMaps;
import com.deathmotion.totemguard.common.config.yaml.YamlMerger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads a {@link ConfigFile} from disk, applies migrations, merges in any missing bundled
 * defaults, and produces a {@link ConfigSnapshot}.
 * <p>
 * Defaults are loaded once per {@link ConfigFile} (cached in {@link #defaultsCache}) and
 * shared across all snapshots of that file.
 */
public final class ConfigService {

    private static final String VERSION_KEY = "config_version";

    private final Path configDir;
    private final ClassLoader classLoader;
    private final MigrationRegistry.Registry migrations;
    private final YamlIO io = new YamlIO();
    private final EnumMap<ConfigFile, BundledResource> defaultsCache = new EnumMap<>(ConfigFile.class);

    public ConfigService(Path configDir, ClassLoader classLoader, MigrationRegistry.Registry migrations) {
        this.configDir = Objects.requireNonNull(configDir, "configDir");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        this.migrations = Objects.requireNonNull(migrations, "migrations");

        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create config directory: " + configDir, e);
        }
    }

    public ConfigSnapshot loadAndMigrate(ConfigFile file) {
        Path target = configDir.resolve(file.fileName());
        BundledResource bundled = loadBundled(file);

        if (Files.notExists(target)) {
            copyVerbatim(file, target);
        }

        String diskText = io.readString(target);
        Map<String, Object> diskMap = io.parseToMap(diskText);

        int diskVersion = io.readVersion(diskMap, VERSION_KEY);
        int latestVersion = io.readVersion(bundled.map, VERSION_KEY);

        Map<String, Object> migratedMap = diskMap;
        boolean migrated = false;
        if (diskVersion < latestVersion) {
            migratedMap = runMigrations(file, diskMap, diskVersion, latestVersion);
            migratedMap.put(VERSION_KEY, latestVersion);
            migrated = true;
        }

        String finalText;
        if (migrated) {
            // Comments are not preserved across a migration; emit a fresh document.
            // The version was already stamped on migratedMap above.
            finalText = YamlMerger.dumpFresh(migratedMap);
        } else {
            // User file is at the bundled version (or higher — a downgrade scenario we leave alone).
            // Surgically insert any missing defaults; the on-disk version line is preserved as-is.
            finalText = YamlMerger.addMissingDefaults(diskText, migratedMap, bundled.map);
        }

        Map<String, Object> finalMap = io.parseToMap(finalText);
        int finalVersion = io.readVersion(finalMap, VERSION_KEY);

        if (!finalText.equals(diskText)) {
            io.writeStringAtomic(target, finalText);
        }

        return new ConfigSnapshot(
                file,
                YamlMaps.toLinkedMap(finalMap),
                finalVersion,
                bundled.resolver
        );
    }

    private Map<String, Object> runMigrations(ConfigFile file, Map<String, Object> startMap, int from, int to) {
        Map<String, Object> map = YamlMaps.toLinkedMap(startMap);
        int v = from;
        while (v < to) {
            ConfigMigration m = migrations.find(file, v);
            if (m == null) {
                throw new IllegalStateException(
                        "Missing migration for " + file + " from version " + v + " to " + (v + 1));
            }
            MigrationContext ctx = new MigrationContext(map);
            m.apply(ctx);
            v = m.toVersion();
        }
        return map;
    }

    private BundledResource loadBundled(ConfigFile file) {
        BundledResource cached = defaultsCache.get(file);
        if (cached != null) return cached;

        try (InputStream in = classLoader.getResourceAsStream(file.fileName())) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled resource: " + file.fileName());
            }
            String text = io.readString(in);
            Map<String, Object> map = io.parseToMap(text);
            BundledResource resource = new BundledResource(text, map, new DefaultsResolver(map));
            defaultsCache.put(file, resource);
            return resource;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bundled resource: " + file.fileName(), e);
        }
    }

    private void copyVerbatim(ConfigFile file, Path target) {
        try (InputStream in = classLoader.getResourceAsStream(file.fileName())) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled resource: " + file.fileName());
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to copy bundled config " + file.fileName() + " to " + target, e);
        }
    }

    private record BundledResource(String text, Map<String, Object> map, DefaultsResolver resolver) {
    }
}
