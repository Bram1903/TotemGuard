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

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.api.config.ConfigSection;
import com.deathmotion.totemguard.common.config.path.PathId;
import com.deathmotion.totemguard.common.config.path.PathResolver;
import com.deathmotion.totemguard.common.config.yaml.YamlMaps;
import com.deathmotion.totemguard.common.config.yaml.YamlNavigator;
import org.jspecify.annotations.NonNull;

import java.util.*;

final class ConfigView implements Config {

    private final ConfigFile file;
    private final int version;
    private final Map<String, Object> root;

    private final YamlNavigator navigator;
    private final PathResolver resolver;

    ConfigView(ConfigFile file, int version, Map<String, Object> root, YamlNavigator navigator, PathResolver resolver) {
        this.file = file;
        this.version = version;
        this.root = root;
        this.navigator = navigator;
        this.resolver = resolver;
    }

    @Override
    public @NonNull ConfigFile file() {
        return file;
    }

    @Override
    public int version() {
        return version;
    }

    @Override
    public boolean contains(@NonNull String path) {
        PathId id = resolver.resolve(path);
        if (id.isInvalid()) return false;
        return navigator.contains(id);
    }

    @Override
    public @NonNull Optional<Object> get(@NonNull String path) {
        PathId id = resolver.resolve(path);
        if (id.isInvalid()) return Optional.empty();
        return navigator.get(id);
    }

    @Override
    public @NonNull Optional<String> getString(@NonNull String path) {
        return get(path).map(String::valueOf);
    }

    @Override
    public @NonNull Optional<Integer> getInt(@NonNull String path) {
        return get(path).flatMap(v -> {
            if (v instanceof Number n) return Optional.of(n.intValue());
            try {
                return Optional.of(Integer.parseInt(String.valueOf(v)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        });
    }

    @Override
    public @NonNull Optional<Boolean> getBoolean(@NonNull String path) {
        return get(path).flatMap(v -> {
            if (v instanceof Boolean b) return Optional.of(b);
            String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
            if ("true".equals(s)) return Optional.of(true);
            if ("false".equals(s)) return Optional.of(false);
            return Optional.empty();
        });
    }

    @Override
    public @NonNull Optional<Double> getDouble(@NonNull String path) {
        return get(path).flatMap(v -> {
            if (v instanceof Number n) return Optional.of(n.doubleValue());
            try {
                return Optional.of(Double.parseDouble(String.valueOf(v)));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        });
    }

    @Override
    public @NonNull List<String> getStringList(@NonNull String path) {
        Object v = get(path).orElse(null);
        if (!(v instanceof List<?> list) || list.isEmpty()) return List.of();

        List<String> out = new ArrayList<>(list.size());
        for (Object o : list) out.add(String.valueOf(o));
        return Collections.unmodifiableList(out);
    }

    @Override
    public @NonNull Optional<ConfigSection> getSection(@NonNull String path) {
        return get(path).flatMap(v -> {
            if (!(v instanceof Map<?, ?> m)) return Optional.empty();
            return Optional.of(new SectionView(YamlMaps.toLinkedMap(m)));
        });
    }

    @Override
    public @NonNull Map<String, Object> asMap() {
        return Collections.unmodifiableMap(root);
    }

    /**
     * Section lookups are RELATIVE to {@code sectionRoot}. They must not depend on the global
     * PathRegistry, because the registry is designed around absolute paths (e.g. "mods.AutoTotem")
     * while sections routinely use relative paths (e.g. "payloads").
     * <p>
     * Relative lookups here do a simple dot-split. This is small and predictable work and avoids
     * correctness issues with dynamic sections (mods, checks, etc).
     */
    private record SectionView(Map<String, Object> sectionRoot) implements ConfigSection {

        @Override
        public boolean contains(@NonNull String path) {
            return get(path).isPresent();
        }

        @Override
        public @NonNull Optional<Object> get(@NonNull String path) {
            String p = path == null ? "" : path.trim();
            if (p.isEmpty()) return Optional.empty();

            String[] tokens = p.split("\\.");
            Object cur = sectionRoot;

            for (String key : tokens) {
                if (!(cur instanceof Map<?, ?> map)) return Optional.empty();
                cur = map.get(key);
                if (cur == null) return Optional.empty();
            }

            return Optional.of(cur);
        }

        @Override
        public @NonNull Optional<String> getString(@NonNull String path) {
            return get(path).map(String::valueOf);
        }

        @Override
        public @NonNull Optional<Integer> getInt(@NonNull String path) {
            return get(path).flatMap(v -> {
                if (v instanceof Number n) return Optional.of(n.intValue());
                try {
                    return Optional.of(Integer.parseInt(String.valueOf(v)));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            });
        }

        @Override
        public @NonNull Optional<Boolean> getBoolean(@NonNull String path) {
            return get(path).flatMap(v -> {
                if (v instanceof Boolean b) return Optional.of(b);
                String s = String.valueOf(v).trim().toLowerCase(Locale.ROOT);
                if ("true".equals(s)) return Optional.of(true);
                if ("false".equals(s)) return Optional.of(false);
                return Optional.empty();
            });
        }

        @Override
        public @NonNull Optional<Double> getDouble(@NonNull String path) {
            return get(path).flatMap(v -> {
                if (v instanceof Number n) return Optional.of(n.doubleValue());
                try {
                    return Optional.of(Double.parseDouble(String.valueOf(v)));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            });
        }

        @Override
        public @NonNull List<String> getStringList(@NonNull String path) {
            Object v = get(path).orElse(null);
            if (!(v instanceof List<?> list) || list.isEmpty()) return List.of();

            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) out.add(String.valueOf(o));
            return Collections.unmodifiableList(out);
        }

        @Override
        public @NonNull Optional<ConfigSection> getSection(@NonNull String path) {
            return get(path).flatMap(v -> {
                if (!(v instanceof Map<?, ?> m)) return Optional.empty();
                return Optional.of(new SectionView(YamlMaps.toLinkedMap(m)));
            });
        }

        @Override
        public @NonNull Map<String, Object> asMap() {
            return Collections.unmodifiableMap(sectionRoot);
        }
    }
}
