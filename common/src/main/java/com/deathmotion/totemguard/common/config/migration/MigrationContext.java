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

package com.deathmotion.totemguard.common.config.migration;

import com.deathmotion.totemguard.common.config.yaml.YamlMaps;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Mutation-friendly handle to a parsed YAML document during migration.
 * <p>
 * Migrations operate on the parsed map directly; the file is re-emitted from this map
 * once all migrations for the file have been applied. This means migrations cannot
 * preserve user-authored comments — but bundled comments reappear via the
 * defaults-merge step that runs after migration.
 */
public record MigrationContext(Map<String, Object> map) {

    public MigrationContext(Map<String, Object> map) {
        this.map = Objects.requireNonNull(map, "map");
    }

    public Optional<Object> get(String dottedPath) {
        return YamlMaps.walk(map, dottedPath);
    }

    public boolean contains(String dottedPath) {
        return get(dottedPath).isPresent();
    }

    /**
     * Sets a value at the given path, creating intermediate maps as needed.
     */
    public void set(String dottedPath, Object value) {
        YamlMaps.setPath(map, dottedPath, value);
    }

    /**
     * Removes a value at the given path. Empty parent maps are left in place.
     */
    public void remove(String dottedPath) {
        YamlMaps.removePath(map, dottedPath);
    }

    /**
     * Renames a path: copies the value to {@code newPath} (if present) and removes
     * {@code oldPath}. No-op when {@code oldPath} is absent.
     */
    public void rename(String oldPath, String newPath) {
        Optional<Object> existing = get(oldPath);
        if (existing.isEmpty()) return;
        set(newPath, existing.get());
        remove(oldPath);
    }

    /**
     * Updates a value at the given path through a transformation function.
     * No-op when the path is absent.
     */
    public void update(String dottedPath, Function<Object, Object> updater) {
        Optional<Object> existing = get(dottedPath);
        if (existing.isEmpty()) return;
        set(dottedPath, updater.apply(existing.get()));
    }
}
