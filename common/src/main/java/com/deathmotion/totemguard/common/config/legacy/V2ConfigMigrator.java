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

import com.deathmotion.totemguard.common.config.yaml.YamlMerger;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Detects a leftover TotemGuard V2 install in or alongside the V3 plugin directory and
 * archives its config files (and embedded H2 database) into an {@code old/} subdirectory.
 * Before archiving, scrapes a small set of carry-over settings (database credentials,
 * Redis credentials, Discord webhooks, server name) and exposes them via a
 * {@link V2Migration} that the caller applies onto the freshly-written V3 configs.
 * <p>
 * Checks and mod-detection sections are intentionally skipped — V3 reworked them.
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

    public @NotNull V2Migration migrate(@NotNull Path v3PluginDir) {
        V2Migration migration = new V2Migration();

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
                migrateDir(dir, migration);
            } catch (Exception ex) {
                logger.log(Level.WARNING,
                        "TotemGuard V2 migration failed for " + dir + " (continuing without it)", ex);
            }
        }

        return migration;
    }

    /**
     * Applies the carry-over settings extracted by {@link #migrate(Path)} onto the V3 config
     * files in {@code v3PluginDir}. Existing V3 default scalars are overwritten in place;
     * comments and structure are preserved.
     */
    public void applyOverrides(@NotNull Path v3PluginDir, @NotNull V2Migration migration) {
        if (migration.isEmpty()) return;

        applyToFile(v3PluginDir.resolve("config.yml"), migration.configOverrides);
        applyToFile(v3PluginDir.resolve("discord.yml"), migration.discordOverrides);

        logger.info("Carried over " + migration.totalCount()
                + " setting(s) from TotemGuard V2 into V3 configs.");
    }

    private void migrateDir(Path dir, V2Migration migration) throws IOException {
        if (!Files.isDirectory(dir)) return;
        if (!hasV2Markers(dir)) return;

        extractFromV2Config(dir.resolve("config.yml"), migration);
        extractFromV2Webhooks(dir.resolve("webhooks.yml"), migration);

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

    private void extractFromV2Config(Path file, V2Migration migration) {
        if (!Files.isRegularFile(file)) return;
        Map<String, Object> root = readYamlMap(file);
        if (root == null) return;

        Object server = root.get("server");
        if (server instanceof String s && !s.isBlank()) {
            migration.configOverrides.put(List.of("server"), s);
        }

        Object command = root.get("command");
        if (command instanceof String cmd && !cmd.isBlank()) {
            migration.configOverrides.put(List.of("commands", "base"), cmd);
        }

        Object aliases = root.get("command-aliases");
        if (aliases instanceof List<?> list) {
            List<String> cleaned = new java.util.ArrayList<>(list.size());
            for (Object item : list) {
                if (item == null) continue;
                String a = String.valueOf(item).trim();
                if (!a.isEmpty()) cleaned.add(a);
            }
            migration.configOverrides.put(List.of("commands", "aliases"), cleaned);
        }

        Object dbRaw = root.get("database");
        if (dbRaw instanceof Map<?, ?> db) {
            String type = stringOrNull(firstPresent(db, "-type", "type"));
            if (type != null) {
                boolean enabled = type.equalsIgnoreCase("mariadb") || type.equalsIgnoreCase("mysql");
                migration.configOverrides.put(List.of("database", "enabled"), enabled);
                if (enabled) {
                    copyIfPresent(db, migration.configOverrides,
                            List.of("-host", "host"), List.of("database", "host"));
                    copyIfPresent(db, migration.configOverrides,
                            List.of("-port", "port"), List.of("database", "port"));
                    copyIfPresent(db, migration.configOverrides,
                            List.of("-name", "name", "database"), List.of("database", "database"));
                    copyIfPresent(db, migration.configOverrides,
                            List.of("-username", "username"), List.of("database", "username"));
                    copyIfPresent(db, migration.configOverrides,
                            List.of("-password", "password"), List.of("database", "password"));
                }
            }
        }

        Object redisRaw = root.get("redis");
        if (redisRaw instanceof Map<?, ?> redis) {
            copyIfPresent(redis, migration.configOverrides,
                    List.of("enabled"), List.of("redis", "enabled"));
            copyIfPresent(redis, migration.configOverrides,
                    List.of("host"), List.of("redis", "host"));
            copyIfPresent(redis, migration.configOverrides,
                    List.of("port"), List.of("redis", "port"));
            copyIfPresent(redis, migration.configOverrides,
                    List.of("username"), List.of("redis", "username"));
            copyIfPresent(redis, migration.configOverrides,
                    List.of("password"), List.of("redis", "password"));
            copyIfPresent(redis, migration.configOverrides,
                    List.of("channel"), List.of("redis", "messaging", "channel"));

            Object syncAlerts = redis.get("sync-alerts");
            if (syncAlerts instanceof Boolean b) {
                migration.configOverrides.put(List.of("redis", "messaging", "send-alerts"), b);
                migration.configOverrides.put(List.of("redis", "messaging", "receive-alerts"), b);
            }
        }
    }

    private void extractFromV2Webhooks(Path file, V2Migration migration) {
        if (!Files.isRegularFile(file)) return;
        Map<String, Object> root = readYamlMap(file);
        if (root == null) return;

        extractWebhookSection(root, "alert", "alerts", migration);
        extractWebhookSection(root, "punishment", "punishments", migration);
    }

    private void extractWebhookSection(Map<String, Object> root, String v2Key, String v3Key, V2Migration migration) {
        Object section = root.get(v2Key);
        if (!(section instanceof Map<?, ?> s)) return;

        copyIfPresent(s, migration.discordOverrides,
                List.of("enabled"), List.of(v3Key, "enabled"));
        copyIfPresent(s, migration.discordOverrides,
                List.of("url"), List.of(v3Key, "url"));
        copyIfPresent(s, migration.discordOverrides,
                List.of("name"), List.of(v3Key, "username"));
        copyIfPresent(s, migration.discordOverrides,
                List.of("color"), List.of(v3Key, "color"));
        copyIfPresent(s, migration.discordOverrides,
                List.of("title"), List.of(v3Key, "title"));
        copyIfPresent(s, migration.discordOverrides,
                List.of("profile-image"), List.of(v3Key, "avatar"));
        copyIfPresent(s, migration.discordOverrides,
                List.of("timestamp"), List.of(v3Key, "timestamp"));
    }

    private void copyIfPresent(Map<?, ?> source,
                               Map<List<String>, Object> dest,
                               List<String> sourceKeyCandidates,
                               List<String> destPath) {
        for (String k : sourceKeyCandidates) {
            if (source.containsKey(k)) {
                Object v = source.get(k);
                if (v != null) {
                    dest.put(destPath, v);
                    return;
                }
            }
        }
    }

    private Object firstPresent(Map<?, ?> source, String... keys) {
        for (String k : keys) {
            if (source.containsKey(k)) {
                Object v = source.get(k);
                if (v != null) return v;
            }
        }
        return null;
    }

    private String stringOrNull(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private Map<String, Object> readYamlMap(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Object root = new Yaml().load(text);
            if (!(root instanceof Map<?, ?> map)) return null;
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                    "Failed to read V2 YAML at " + file + " (skipping carry-over)", ex);
            return null;
        }
    }

    private void applyToFile(Path file, Map<List<String>, Object> overrides) {
        if (overrides.isEmpty() || !Files.isRegularFile(file)) return;
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            String original = text;
            for (Map.Entry<List<String>, Object> e : overrides.entrySet()) {
                text = YamlMerger.setValueAtPath(text, e.getKey(), e.getValue());
            }
            if (!text.equals(original)) {
                Files.writeString(file, text, StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to apply V2 carry-over to " + file, ex);
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

    /**
     * Carry-over settings scraped from a V2 install. Keys are nested paths into the V3
     * {@code config.yml} or {@code discord.yml} (e.g. {@code [redis, messaging, channel]}).
     */
    public static final class V2Migration {
        private final Map<List<String>, Object> configOverrides = new LinkedHashMap<>();
        private final Map<List<String>, Object> discordOverrides = new LinkedHashMap<>();

        public boolean isEmpty() {
            return configOverrides.isEmpty() && discordOverrides.isEmpty();
        }

        public int totalCount() {
            return configOverrides.size() + discordOverrides.size();
        }
    }
}
