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

import com.deathmotion.totemguard.api.config.ConfigFile;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class MigrationRegistry {

    private MigrationRegistry() {
    }

    public static Registry buildDefault() {
        Registry r = new Registry();

        // Gotta add migrations here, once we release new versions that change the config based on version 1

        return r;
    }

    public static final class Registry {

        private final Map<ConfigFile, Map<Integer, ConfigMigration>> migrations = new EnumMap<>(ConfigFile.class);

        public void register(ConfigFile file, ConfigMigration migration) {
            if (migration.toVersion() != migration.fromVersion() + 1) {
                throw new IllegalArgumentException(
                        "Migration must advance exactly one version: " + file
                                + " " + migration.fromVersion() + " -> " + migration.toVersion());
            }
            ConfigMigration existing = migrations.computeIfAbsent(file, f -> new HashMap<>())
                    .putIfAbsent(migration.fromVersion(), migration);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate migration for " + file + " from version " + migration.fromVersion());
            }
        }

        public ConfigMigration find(ConfigFile file, int fromVersion) {
            Map<Integer, ConfigMigration> m = migrations.get(file);
            if (m == null) return null;
            return m.get(fromVersion);
        }
    }
}
