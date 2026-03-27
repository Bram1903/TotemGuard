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

package com.deathmotion.totemguard.common.check.impl.mods;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigSection;
import com.deathmotion.totemguard.common.TGPlatform;

import java.util.*;

public final class ModRegistry {

    private static final String MODS_ROOT_PATH = "mods";
    private static final String PAYLOADS_PATH = "payloads";
    private static final String SEVERITY_PATH = "severity";

    private static volatile Map<String, ModDefinition> DEFINITIONS = Map.of();

    private ModRegistry() {
    }

    public static Map<String, ModDefinition> getDefinitions() {
        return DEFINITIONS;
    }

    public static void load(Config config) {
        final LinkedHashMap<String, ModDefinition> loadedDefinitions = new LinkedHashMap<>();

        config.getSection(MODS_ROOT_PATH).ifPresent(modsSection -> {
            for (String modId : modsSection.asMap().keySet()) {
                final String normalizedModId = normalizeModId(modId);
                if (normalizedModId == null) {
                    warn("Ignoring mod entry with an empty id.");
                    continue;
                }

                final Optional<ConfigSection> modSection = modsSection.getSection(modId);
                if (modSection.isEmpty()) {
                    warn("Ignoring mod '" + normalizedModId + "' because it is not a section.");
                    continue;
                }

                final ModDefinition definition = parseDefinition(normalizedModId, modSection.get());
                if (definition == null) {
                    continue;
                }

                loadedDefinitions.put(normalizedModId, definition);
            }
        });

        DEFINITIONS = Collections.unmodifiableMap(new LinkedHashMap<>(loadedDefinitions));
    }

    private static ModDefinition parseDefinition(String modId, ConfigSection modSection) {
        final List<String> payloads = normalizeValues(modSection.getStringList(PAYLOADS_PATH), true);

        if (payloads.isEmpty()) {
            warn("Ignoring mod '" + modId + "' because it has no payloads.");
            return null;
        }

        final String severityValue = modSection.getString(SEVERITY_PATH).orElse(null);
        final ModSeverity severity = ModSeverity.fromConfigValue(severityValue);
        if (severity == null) {
            warn("Ignoring invalid severity '" + severityValue + "' for mod '" + modId
                    + "'. Valid values: LOG, KICK, BAN. Using KICK.");
        }

        return new ModDefinition(
                modId,
                severity == null ? ModSeverity.KICK : severity,
                payloads
        );
    }

    private static List<String> normalizeValues(List<String> values, boolean lowerCase) {
        if (values.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<String> normalizedValues = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }

            String normalized = value.trim();
            if (normalized.isBlank()) {
                continue;
            }

            if (lowerCase) {
                normalized = normalized.toLowerCase(Locale.ROOT);
            }

            normalizedValues.add(normalized);
        }

        if (normalizedValues.isEmpty()) {
            return List.of();
        }

        return List.copyOf(normalizedValues);
    }

    private static String normalizeModId(String modId) {
        if (modId == null) {
            return null;
        }

        final String normalized = modId.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static void warn(String message) {
        TGPlatform.getInstance().getLogger().warning("[mods.yml] " + message);
    }
}
