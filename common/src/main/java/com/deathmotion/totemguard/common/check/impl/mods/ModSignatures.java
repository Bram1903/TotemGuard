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

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

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

    public static void load(CommentedConfigurationNode mods) {
        CommentedConfigurationNode modsNode = mods.node(MODS_ROOT_KEY);

        Map<String, ModSignature> loaded = new LinkedHashMap<>();

        for (var entry : modsNode.childrenMap().entrySet()) {
            String modId = String.valueOf(entry.getKey());
            if (modId.isBlank()) continue;

            CommentedConfigurationNode modNode = entry.getValue();
            if (modNode.virtual()) continue;

            List<String> payloads = normalizeList(readStringList(modNode.node(PAYLOAD_KEY)));
            List<String> translations = normalizeList(readStringList(modNode.node(TRANSLATIONS_KEY)));

            if (payloads.isEmpty() && translations.isEmpty()) continue;

            loaded.put(modId, new ModSignature(payloads, translations));
        }

        SIGNATURES = Map.copyOf(loaded);
    }

    private static List<String> readStringList(CommentedConfigurationNode node) {
        try {
            return node.getList(String.class, List.of());
        } catch (SerializationException ignored) {
            return List.of();
        }
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

