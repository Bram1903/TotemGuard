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

package com.deathmotion.totemguard.common.commands;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.CommandsOptions;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import lombok.experimental.UtilityClass;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;

@UtilityClass
public class CommandDefaults {

    public static final String PERMISSION_PREFIX = "TotemGuardV3.";

    private static final CommandsOptions OPTIONS = TGPlatform.getInstance()
            .getConfigRepository()
            .configView()
            .commands();

    public static final String ROOT = OPTIONS.base().isBlank() ? "totemguard" : OPTIONS.base();
    public static final String[] ALIASES = OPTIONS.aliases().stream()
            .filter(s -> !s.isBlank())
            .toArray(String[]::new);

    public static Command.Builder<Sender> root(final CommandManager<Sender> manager) {
        return manager.commandBuilder(
                ROOT,
                Description.of("The root command for TotemGuard."),
                ALIASES
        );
    }
}
