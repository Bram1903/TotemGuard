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

package com.deathmotion.totemguard.loader.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class LoaderConfig {

    private final String version;
    private final Source source;

    private LoaderConfig(String version, Source source) {
        this.version = version;
        this.source = source;
    }

    public static LoaderConfig loadOrWriteDefault(Path loaderDir, Logger logger) throws IOException {
        Files.createDirectories(loaderDir);
        Path configFile = loaderDir.resolve("loader-config.yml");
        if (!Files.exists(configFile)) {
            try (InputStream defaultStream = LoaderConfig.class.getResourceAsStream("/loader-config.yml")) {
                if (defaultStream == null) {
                    throw new IOException("Loader default config resource is missing from the jar.");
                }
                Files.copy(defaultStream, configFile);
            }
            logger.info("loader-config.yml: wrote default at " + configFile + ". Edit and restart to change the source.");
        }

        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(configFile)) {
            Object parsed = yaml.load(in);
            if (!(parsed instanceof Map<?, ?> map)) {
                throw new IOException("loader-config.yml is empty or malformed.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            root = typed;
        }

        String version = asString(root.get("version"), "LATEST").trim();
        if (version.isEmpty()) version = "LATEST";

        String sourceRaw = asString(root.get("source"), "GITHUB").trim().toUpperCase(Locale.ROOT);
        Source source;
        try {
            source = Source.valueOf(sourceRaw);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid 'source' in loader-config.yml: '" + sourceRaw
                    + "'. Expected GITHUB or MODRINTH.");
        }

        return new LoaderConfig(version, source);
    }

    private static String asString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    public String version() {
        return version;
    }

    public Source source() {
        return source;
    }

    public boolean isGit() {
        return "GIT".equalsIgnoreCase(version);
    }

    public Source effectiveSource() {
        return isGit() ? Source.GITHUB : source;
    }

    /**
     * If the configured version is a moving channel (LATEST, EXPERIMENTAL, GIT),
     * returns its canonical upper-case name. Returns {@code null} for pinned versions
     * like {@code 3.0.0}.
     */
    public String channel() {
        String upper = version.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "LATEST", "EXPERIMENTAL", "GIT" -> upper;
            default -> null;
        };
    }

    public enum Source {GITHUB, MODRINTH, LOCAL}
}
