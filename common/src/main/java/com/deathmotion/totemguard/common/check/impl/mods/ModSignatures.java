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

import com.deathmotion.totemguard.api.config.Config;

import java.util.*;

public final class ModSignatures {

    private static final String MODS_ROOT_KEY = "mods";
    private static final String PAYLOAD_KEY = "payloads";
    private static final String TRANSLATIONS_KEY = "translations";

    private static volatile Map<String, ModSignature> SIGNATURES = Map.of();

    private ModSignatures() {
    }

    public static Map<String, ModSignature> get() {
        return SIGNATURES;
    }

    public static void load(Config mods) {
        Map<String, ModSignature> loaded = new LinkedHashMap<>();

        mods.getSection(MODS_ROOT_KEY).ifPresent(modsSection -> {
            for (String modId : modsSection.asMap().keySet()) {
                if (modId == null || modId.isBlank()) continue;

                modsSection.getSection(modId).ifPresent(modSection -> {
                    List<String> payloads = normalizeList(modSection.getStringList(PAYLOAD_KEY));
                    List<String> translations = normalizeList(modSection.getStringList(TRANSLATIONS_KEY));

                    if (payloads.isEmpty() && translations.isEmpty()) return;

                    loaded.put(modId, new ModSignature(payloads, translations));
                });
            }
        });

        SIGNATURES = Map.copyOf(loaded);
    }

    private static List<String> normalizeList(List<String> input) {
        if (input == null || input.isEmpty()) return List.of();
        return input.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isBlank())
                .toList();
    }
}
