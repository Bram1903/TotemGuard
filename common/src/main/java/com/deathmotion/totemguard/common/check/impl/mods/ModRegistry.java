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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.ModConfig;
import com.deathmotion.totemguard.common.config.view.ModsView;

import java.util.*;

public final class ModRegistry {

    private static volatile Map<String, ModDefinition> DEFINITIONS = Map.of();

    private ModRegistry() {
    }

    public static Map<String, ModDefinition> getDefinitions() {
        return DEFINITIONS;
    }

    public static void load() {
        ModsView view = TGPlatform.getInstance().getConfigRepository().mods();

        LinkedHashMap<String, ModDefinition> loaded = new LinkedHashMap<>();
        for (ModConfig cfg : view.all().values()) {
            List<String> payloads = normalizePayloads(cfg.payloads());
            if (payloads.isEmpty()) {
                warn("Ignoring mod '" + cfg.id() + "' because it has no payloads.");
                continue;
            }

            ModSeverity severity = ModSeverity.fromConfigValue(cfg.severity());
            if (severity == null) {
                warn("Ignoring invalid severity '" + cfg.severity() + "' for mod '" + cfg.id()
                        + "'. Valid values: LOG, KICK, BAN, KICK_THEN_BAN. Using KICK.");
                severity = ModSeverity.KICK;
            }

            loaded.put(cfg.id(), new ModDefinition(cfg.id(), severity, payloads));
        }

        DEFINITIONS = Collections.unmodifiableMap(loaded);
    }

    private static List<String> normalizePayloads(List<String> values) {
        if (values.isEmpty()) return List.of();

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) continue;
            String trimmed = value.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) normalized.add(trimmed);
        }
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    private static void warn(String message) {
        TGPlatform.getInstance().getLogger().warning("[mods.yml] " + message);
    }
}
