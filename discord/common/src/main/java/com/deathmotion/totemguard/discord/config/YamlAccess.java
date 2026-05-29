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

package com.deathmotion.totemguard.discord.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class YamlAccess {
    private final Map<String, Object> root;

    private YamlAccess(@NotNull Map<String, Object> root) {
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    static @NotNull YamlAccess of(@Nullable Object raw) {
        if (raw instanceof Map<?, ?> map) {
            return new YamlAccess((Map<String, Object>) map);
        }
        return new YamlAccess(Map.of());
    }

    private static @Nullable Long parseLong(@Nullable Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return null;
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @NotNull YamlAccess section(@NotNull String key) {
        Object value = root.get(key);
        return value instanceof Map<?, ?> map ? new YamlAccess((Map<String, Object>) map) : of(null);
    }

    @NotNull String string(@NotNull String key, @NotNull String def) {
        Object value = root.get(key);
        return value == null ? def : String.valueOf(value);
    }

    boolean bool(@NotNull String key, boolean def) {
        Object value = root.get(key);
        if (value instanceof Boolean b) return b;
        if (value == null) return def;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    int integer(@NotNull String key, int def) {
        Object value = root.get(key);
        if (value instanceof Number n) return n.intValue();
        if (value == null) return def;
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @NotNull List<Long> longList(@NotNull String key) {
        Object value = root.get(key);
        if (!(value instanceof List<?> list)) return List.of();
        List<Long> out = new ArrayList<>(list.size());
        for (Object entry : list) {
            Long parsed = parseLong(entry);
            if (parsed != null) out.add(parsed);
        }
        return out;
    }

    long longValue(@NotNull String key, long def) {
        Long parsed = parseLong(root.get(key));
        return parsed == null ? def : parsed;
    }
}
