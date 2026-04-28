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

package com.deathmotion.totemguard.common.config.view;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigSection;
import com.deathmotion.totemguard.common.config.schema.ModConfig;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Internal typed view of {@code mods.yml}.
 */
public final class ModsView {

    private static final String DEFAULT_SEVERITY = "KICK";

    private final int version;
    private final Map<String, ModConfig> mods;

    public ModsView(Config config) {
        this.version = config.version();

        Map<String, ModConfig> parsed = new LinkedHashMap<>();
        config.getSection("mods").ifPresent(modsSection -> {
            for (String modId : modsSection.asMap().keySet()) {
                String trimmed = modId.trim();
                if (trimmed.isEmpty()) continue;

                Optional<ConfigSection> entry = modsSection.getSection(modId);
                if (entry.isEmpty()) continue;

                List<String> payloads = entry.get().getStringList("payloads");
                List<String> translations = entry.get().getStringList("translations");
                if (payloads.isEmpty() && translations.isEmpty()) continue;

                String severity = entry.get().getString("severity").orElse(DEFAULT_SEVERITY);

                parsed.put(trimmed, new ModConfig(trimmed, severity, payloads, translations));
            }
        });
        this.mods = Collections.unmodifiableMap(parsed);
    }

    public int version() {
        return version;
    }

    public @NotNull Map<String, ModConfig> all() {
        return mods;
    }
}
