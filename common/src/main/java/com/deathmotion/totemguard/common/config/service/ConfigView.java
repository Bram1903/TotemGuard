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

package com.deathmotion.totemguard.common.config.service;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.ConfigSection;
import com.deathmotion.totemguard.api3.config.key.ConfigKey;
import com.deathmotion.totemguard.common.config.yaml.DefaultsResolver;
import com.deathmotion.totemguard.common.config.yaml.YamlMaps;
import org.jetbrains.annotations.NotNull;

import java.util.*;

final class ConfigView implements Config {

    private final ConfigFile file;
    private final int version;
    private final Map<String, Object> root;
    private final DefaultsResolver defaults;

    ConfigView(ConfigFile file, int version, Map<String, Object> root, DefaultsResolver defaults) {
        this.file = file;
        this.version = version;
        this.root = root;
        this.defaults = defaults;
    }

    private static IllegalStateException typeMismatch(ConfigKey<?> key, Object value, String expected) {
        return new IllegalStateException(
                "Config value for " + key.path() + " is not a " + expected + ": "
                        + (value == null ? "null" : value.getClass().getSimpleName() + "(" + value + ")"));
    }

    private static Optional<Integer> asInt(Object v) {
        if (v instanceof Number n) return Optional.of(n.intValue());
        try {
            return Optional.of(Integer.parseInt(String.valueOf(v).trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Boolean> asBool(Object v) {
        if (v instanceof Boolean b) return Optional.of(b);
        String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(s)) return Optional.of(true);
        if ("false".equals(s)) return Optional.of(false);
        return Optional.empty();
    }

    private static List<String> asStringList(Object v) {
        if (!(v instanceof List<?> list) || list.isEmpty()) return List.of();
        List<String> out = new ArrayList<>(list.size());
        for (Object o : list) out.add(String.valueOf(o));
        return Collections.unmodifiableList(out);
    }

    @Override
    public @NotNull ConfigFile file() {
        return file;
    }

    @Override
    public int version() {
        return version;
    }

    @Override
    public boolean contains(@NotNull String path) {
        return YamlMaps.walk(root, path).isPresent();
    }

    @Override
    public @NotNull Optional<Object> get(@NotNull String path) {
        return YamlMaps.walk(root, path);
    }

    @Override
    public @NotNull Optional<String> getString(@NotNull String path) {
        return get(path).map(String::valueOf);
    }

    @Override
    public @NotNull Optional<Integer> getInt(@NotNull String path) {
        return get(path).flatMap(ConfigView::asInt);
    }

    @Override
    public @NotNull Optional<Boolean> getBoolean(@NotNull String path) {
        return get(path).flatMap(ConfigView::asBool);
    }

    @Override
    public @NotNull List<@NotNull String> getStringList(@NotNull String path) {
        return get(path).map(ConfigView::asStringList).orElse(List.of());
    }

    @Override
    public @NotNull Optional<ConfigSection> getSection(@NotNull String path) {
        return get(path).flatMap(v -> {
            if (!(v instanceof Map<?, ?> m)) return Optional.empty();
            return Optional.of(new SectionView(YamlMaps.toLinkedMap(m)));
        });
    }

    @Override
    public @NotNull Map<String, Object> asMap() {
        return Collections.unmodifiableMap(root);
    }

    @Override
    public @NotNull String getString(@NotNull ConfigKey<String> key) {
        Object v = resolve(key);
        return v == null ? "" : String.valueOf(v);
    }

    @Override
    public int getInt(@NotNull ConfigKey<Integer> key) {
        Object v = resolve(key);
        if (v == null) return 0;
        return asInt(v).orElseThrow(() -> typeMismatch(key, v, "int"));
    }

    @Override
    public boolean getBoolean(@NotNull ConfigKey<Boolean> key) {
        Object v = resolve(key);
        if (v == null) return false;
        return asBool(v).orElseThrow(() -> typeMismatch(key, v, "boolean"));
    }

    @Override
    public @NotNull List<@NotNull String> getStringList(@NotNull ConfigKey<List<String>> key) {
        Object v = resolve(key);
        return v == null ? List.of() : asStringList(v);
    }

    /**
     * Resolves the value for a typed key, in priority order:
     * <ol>
     *     <li>Non-null value present in the user file → return it.</li>
     *     <li>Non-null value present in the bundled defaults → return it.</li>
     *     <li>Key declared in the bundled defaults (possibly with a YAML-null value, e.g.
     *     {@code api-key:}) → return null, which callers map to a type-appropriate empty.</li>
     *     <li>Otherwise → throw, indicating a programming error.</li>
     * </ol>
     */
    private Object resolve(ConfigKey<?> key) {
        Object v = YamlMaps.walk(root, key.path()).orElse(null);
        if (v != null) return v;
        v = defaults.get(key.path()).orElse(null);
        if (v != null) return v;
        if (defaults.contains(key.path()) || YamlMaps.containsPath(root, key.path())) {
            return null;
        }
        throw new IllegalStateException(
                "Missing config value with no bundled default: " + key.path() + " in " + file.fileName());
    }

    /**
     * Section views work on relative paths. They do not consult bundled defaults — sections
     * are typically used for dynamic content (mods, checks) where keys are not known ahead
     * of time and there is no per-key default.
     */
    private record SectionView(Map<String, Object> sectionRoot) implements ConfigSection {

        @Override
        public boolean contains(@NotNull String path) {
            return YamlMaps.walk(sectionRoot, path).isPresent();
        }

        @Override
        public @NotNull Optional<Object> get(@NotNull String path) {
            return YamlMaps.walk(sectionRoot, path);
        }

        @Override
        public @NotNull Optional<String> getString(@NotNull String path) {
            return get(path).map(String::valueOf);
        }

        @Override
        public @NotNull Optional<Integer> getInt(@NotNull String path) {
            return get(path).flatMap(ConfigView::asInt);
        }

        @Override
        public @NotNull Optional<Boolean> getBoolean(@NotNull String path) {
            return get(path).flatMap(ConfigView::asBool);
        }

        @Override
        public @NotNull List<String> getStringList(@NotNull String path) {
            return get(path).map(ConfigView::asStringList).orElse(List.of());
        }

        @Override
        public @NotNull Optional<ConfigSection> getSection(@NotNull String path) {
            return get(path).flatMap(v -> {
                if (!(v instanceof Map<?, ?> m)) return Optional.empty();
                return Optional.of(new SectionView(YamlMaps.toLinkedMap(m)));
            });
        }

        @Override
        public @NotNull Map<String, Object> asMap() {
            return Collections.unmodifiableMap(sectionRoot);
        }
    }
}
