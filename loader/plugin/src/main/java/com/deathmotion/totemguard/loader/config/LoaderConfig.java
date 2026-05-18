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

import com.deathmotion.totemguard.loader.core.HostPlatform;
import com.deathmotion.totemguard.loader.core.PluginVersionGate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class LoaderConfig {

    private final String version;
    private final Source source;
    private final String fleetSharedSecret;

    private LoaderConfig(String version, Source source, String fleetSharedSecret) {
        this.version = version;
        this.source = source;
        this.fleetSharedSecret = fleetSharedSecret;
    }

    public static LoaderConfig loadOrWriteDefault(Path loaderDir, Logger logger, HostPlatform platform) throws IOException {
        // Branch into a separate class for each format so the JVM never resolves the
        // unused parser. Paper bundles snakeyaml at runtime, Fabric bundles gson, and
        // neither bundles the other.
        if (platform == HostPlatform.FABRIC) {
            return JsonLoaderConfigIO.load(loaderDir, logger);
        }
        return YamlLoaderConfigIO.load(loaderDir, logger);
    }

    static LoaderConfig fromMap(Map<String, Object> root, String sourceName, Logger logger) throws IOException {
        String version = asString(root.get("version"), "LATEST").trim();
        if (version.isEmpty()) version = "LATEST";

        String sourceRaw = asString(root.get("source"), "GITHUB").trim().toUpperCase(Locale.ROOT);
        Source source;
        try {
            source = Source.valueOf(sourceRaw);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid 'source' in " + sourceName + ": '" + sourceRaw
                    + "'. Expected GITHUB or MODRINTH.");
        }

        String fleetSecret = readFleetSecret(root);
        if (fleetSecret == null || fleetSecret.isEmpty()) {
            logger.warning(sourceName + ": fleet.shared-secret is not set. "
                    + "Fleet messages will be authenticated trust-on-first-use only. "
                    + "If your Redis is on a shared network, set fleet.shared-secret to a 32+ byte random string on every node.");
        }

        PluginVersionGate.rejectIfPinnedTooOld(version, sourceName);
        return new LoaderConfig(version, source, fleetSecret);
    }

    @SuppressWarnings("unchecked")
    private static String readFleetSecret(Map<String, Object> root) {
        Object fleet = root.get("fleet");
        if (fleet instanceof Map<?, ?> map) {
            Object secret = ((Map<String, Object>) map).get("shared-secret");
            if (secret != null) {
                String trimmed = secret.toString().trim();
                return trimmed.isEmpty() ? null : trimmed;
            }
        }
        return null;
    }

    private static String asString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    public LoaderConfig withVersion(String overrideVersion) {
        return new LoaderConfig(overrideVersion, source, fleetSharedSecret);
    }

    public String fleetSharedSecret() {
        return fleetSharedSecret;
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

    public String channel() {
        String upper = version.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "LATEST", "EXPERIMENTAL", "GIT" -> upper;
            default -> null;
        };
    }

    public enum Source {GITHUB, MODRINTH, LOCAL}
}
