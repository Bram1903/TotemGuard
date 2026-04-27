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

package com.deathmotion.totemguard.common.config.legacy;

import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects a leftover TotemGuard V2 install in or alongside the V3 plugin directory and
 * archives its config files (and embedded H2 database) into an {@code old/} subdirectory.
 * <p>
 * Idempotent: a directory whose recognised V2 artefacts have already been moved is
 * skipped on subsequent runs.
 */
public final class V2ConfigMigrator {

    private static final List<String> V2_FILES = List.of(
            "config.yml",
            "checks.yml",
            "messages.yml",
            "webhooks.yml"
    );

    private static final List<String> V2_DIRS = List.of("db");

    private static final List<String> V2_ONLY_FILES = List.of("webhooks.yml");

    private static final List<String> SHARED_FILES = List.of("config.yml", "checks.yml", "messages.yml");

    private static final String V3_VERSION_KEY = "config_version";

    private static final String OLD_DIR = "old";

    private static final String[] LEGACY_SIBLING_NAMES = {"TotemGuard", "totemguard"};

    private final Logger logger;

    public V2ConfigMigrator(@NotNull Logger logger) {
        this.logger = logger;
    }

    public void migrate(@NotNull Path v3PluginDir) {
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(v3PluginDir);

        Path parent = v3PluginDir.getParent();
        if (parent != null) {
            for (String name : LEGACY_SIBLING_NAMES) {
                Path sibling = parent.resolve(name);
                if (!sibling.equals(v3PluginDir) && Files.isDirectory(sibling)) {
                    candidates.add(sibling);
                }
            }
        }

        for (Path dir : candidates) {
            try {
                migrateDir(dir);
            } catch (Exception ex) {
                logger.log(Level.WARNING,
                        "TotemGuard V2 migration failed for " + dir + " (continuing without it)", ex);
            }
        }
    }

    private void migrateDir(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        if (!hasV2Markers(dir)) return;

        Path archive = dir.resolve(OLD_DIR);
        Files.createDirectories(archive);

        int moved = 0;
        for (String file : V2_FILES) {
            if (moveIfPresent(dir.resolve(file), archive.resolve(file))) moved++;
        }
        for (String d : V2_DIRS) {
            if (moveIfPresent(dir.resolve(d), archive.resolve(d))) moved++;
        }

        if (moved > 0) {
            logger.info("Archived " + moved + " TotemGuard V2 entr"
                    + (moved == 1 ? "y" : "ies") + " from " + dir + " into " + archive);
        }
    }

    private boolean hasV2Markers(Path dir) {
        for (String file : V2_ONLY_FILES) {
            if (Files.isRegularFile(dir.resolve(file))) return true;
        }
        for (String d : V2_DIRS) {
            if (Files.isDirectory(dir.resolve(d))) return true;
        }
        for (String file : SHARED_FILES) {
            Path p = dir.resolve(file);
            if (Files.isRegularFile(p) && lacksV3VersionKey(p)) return true;
        }
        return false;
    }

    private boolean lacksV3VersionKey(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Object root = new Yaml().load(text);
            if (!(root instanceof Map<?, ?> map)) return true;
            return !map.containsKey(V3_VERSION_KEY);
        } catch (Exception ex) {
            // Unparseable yaml is treated as legacy; archiving it is the safer choice
            // than leaving a malformed file in place for V3 to choke on.
            return true;
        }
    }

    private boolean moveIfPresent(Path source, Path target) throws IOException {
        if (!Files.exists(source)) return false;
        Path resolved = uniqueTarget(target);
        try {
            Files.move(source, resolved, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicFailure) {
            Files.move(source, resolved);
        }
        return true;
    }

    private Path uniqueTarget(Path target) {
        if (!Files.exists(target)) return target;
        Path parent = target.getParent();
        String name = target.getFileName().toString();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        long stamp = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            String suffix = i == 0 ? "-" + stamp : "-" + stamp + "-" + i;
            Path candidate = parent.resolve(base + suffix + ext);
            if (!Files.exists(candidate)) return candidate;
        }
        throw new IllegalStateException("Could not pick a unique archive name for " + target);
    }
}
