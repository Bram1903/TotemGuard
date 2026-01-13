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

package com.deathmotion.totemguard.common.config.yaml;

import com.deathmotion.totemguard.common.config.path.PathId;
import com.deathmotion.totemguard.common.config.path.PathRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class YamlNavigator {

    private final Map<String, Object> root;
    private final PathRegistry registry;

    public YamlNavigator(Map<String, Object> root, PathRegistry registry) {
        this.root = Objects.requireNonNull(root, "root");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public Optional<Object> get(PathId id) {
        String[] tokens = registry.tokens(id);
        if (tokens.length == 0) return Optional.empty();

        Object cur = root;
        for (String key : tokens) {
            if (!(cur instanceof Map<?, ?> map)) return Optional.empty();
            cur = map.get(key);
            if (cur == null) return Optional.empty();
        }
        return Optional.of(cur);
    }

    public boolean contains(PathId id) {
        return get(id).isPresent();
    }
}
