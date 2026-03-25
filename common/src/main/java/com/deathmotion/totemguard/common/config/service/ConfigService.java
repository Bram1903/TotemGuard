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

import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.common.config.migration.MigrationRegistry;
import com.deathmotion.totemguard.common.config.migration.MutableYaml;
import com.deathmotion.totemguard.common.config.path.PathRegistry;
import com.deathmotion.totemguard.common.config.yaml.YamlIO;
import com.deathmotion.totemguard.common.config.yaml.YamlMaps;
import com.deathmotion.totemguard.common.config.yaml.YamlPatcher;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;

public final class ConfigService {

    private static final String VERSION_KEY = "config_version";

    private final Path configDir;
    private final ClassLoader classLoader;
    private final MigrationRegistry.Registry migrations;

    private final YamlIO io = new YamlIO();

    public ConfigService(
            Path configDir,
            ClassLoader classLoader,
            MigrationRegistry.Registry migrations
    ) {
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

        if (Files.notExists(target)) {
            copyDefaultVerbatim(file, target);
        }

        String diskText = io.readString(target);
        String defaultsText = readDefaultText(file);

        Map<String, Object> diskMap = io.parseToMap(diskText);
        Map<String, Object> defaultsMap = io.parseToMap(defaultsText);

        int diskVersion = io.readVersion(diskMap, VERSION_KEY);
        int latestVersion = io.readVersion(defaultsMap, VERSION_KEY);

        String patchedText = diskText;
        int v = diskVersion;

        if (v < latestVersion) {
            while (v < latestVersion) {
                var migration = migrations.find(file, v);
                if (migration == null) {
                    throw new IllegalStateException(
                            "Missing migration for " + file +
                                    " from version " + v + " to " + (v + 1)
                    );
                }

                Map<String, Object> currentMap = io.parseToMap(patchedText);
                MutableYaml mutable = new MutableYaml(patchedText, currentMap);

                migration.apply(mutable);

                patchedText = mutable.text();
                patchedText = YamlPatcher.setScalar(
                        patchedText,
                        VERSION_KEY,
                        String.valueOf(migration.toVersion())
                );

                v = migration.toVersion();
            }
        }

        Map<String, Object> afterMigrationMap = io.parseToMap(patchedText);

        String mergedText = YamlPatcher.addMissingDefaults(
                patchedText,
                afterMigrationMap,
                defaultsMap
        );

        boolean changed = !mergedText.equals(diskText);

        Map<String, Object> mergedMap = io.parseToMap(mergedText);
        int finalVersion = io.readVersion(mergedMap, VERSION_KEY);

        if (finalVersion != latestVersion) {
            mergedText = YamlPatcher.setScalar(
                    mergedText,
                    VERSION_KEY,
                    String.valueOf(latestVersion)
            );
            mergedMap = io.parseToMap(mergedText);
            changed = true;
            finalVersion = latestVersion;
        }

        if (changed) {
            io.writeStringAtomic(target, mergedText);
        }

        Map<String, Object> normalized = YamlMaps.toLinkedMap(mergedMap);

        PathRegistry registry = new PathRegistry();
        registry.registerAllPaths(normalized);
        registry.registerAllPaths(YamlMaps.toLinkedMap(defaultsMap));

        return new ConfigSnapshot(
                file,
                target,
                normalized,
                finalVersion,
                registry
        );
    }

    private void copyDefaultVerbatim(ConfigFile file, Path target) {
        try (InputStream in = classLoader.getResourceAsStream(file.fileName())) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + file.fileName());
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to copy default config " + file.fileName() + " to " + target, e
            );
        }
    }

    private String readDefaultText(ConfigFile file) {
        try (InputStream in = classLoader.getResourceAsStream(file.fileName())) {
            if (in == null) {
                throw new IllegalStateException("Missing resource: " + file.fileName());
            }
            return io.readString(in);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read default resource " + file.fileName(), e
            );
        }
    }
}
