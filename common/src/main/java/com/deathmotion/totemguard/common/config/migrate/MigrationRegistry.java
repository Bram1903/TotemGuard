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

package com.deathmotion.totemguard.common.config.migrate;

import com.deathmotion.totemguard.common.config.files.ConfigFileKey;
import com.deathmotion.totemguard.common.config.migrate.impl.ChecksMigrations;
import com.deathmotion.totemguard.common.config.migrate.impl.MainConfigMigrations;
import com.deathmotion.totemguard.common.config.migrate.impl.MessagesMigrations;
import org.spongepowered.configurate.transformation.ConfigurationTransformation;

public final class MigrationRegistry {

    public ConfigurationTransformation.Versioned forFile(ConfigFileKey key) {
        return switch (key) {
            case MAIN -> MainConfigMigrations.create();
            case CHECKS -> ChecksMigrations.create();
            case MESSAGES -> MessagesMigrations.create();
        };
    }
}

