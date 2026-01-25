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

import com.deathmotion.totemguard.api3.config.ConfigFile;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class MigrationRegistry {

    public Registry buildDefault() {
        Registry r = new Registry();

        // TODO: We can later add migrations here like this:
        // r.register(ConfigFile.MESSAGES, new Migration_1_to_2_Messages());

        return r;
    }

    public static final class Registry {
        private final Map<ConfigFile, Map<Integer, ConfigMigration>> migrations = new EnumMap<>(ConfigFile.class);

        public void register(ConfigFile file, ConfigMigration migration) {
            migrations.computeIfAbsent(file, f -> new HashMap<>())
                    .put(migration.fromVersion(), migration);
        }

        public ConfigMigration find(ConfigFile file, int fromVersion) {
            Map<Integer, ConfigMigration> m = migrations.get(file);
            if (m == null) return null;
            return m.get(fromVersion);
        }
    }
}
