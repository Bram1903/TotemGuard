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

import com.deathmotion.totemguard.api.config.Config;
import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.api.config.key.impl.ConfigKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.sender.Sender;
import lombok.experimental.UtilityClass;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.description.Description;

import java.util.List;

@UtilityClass
public class CommandDefaults {

    public static final String PERMISSION_PREFIX = "totemguard.";

    private static final Config CONFIG = TGPlatform.getInstance()
            .getConfigRepository()
            .config(ConfigFile.CONFIG);

    public static final String ROOT = readRoot();
    public static final String[] ALIASES = readAliases();

    public static Command.Builder<Sender> root(final CommandManager<Sender> manager) {
        return manager.commandBuilder(
                ROOT,
                Description.of("The root command for TotemGuard."),
                ALIASES
        );
    }

    private static String readRoot() {
        String root = CONFIG.getString(ConfigKeys.COMMANDS_BASE);
        return root.isBlank() ? ConfigKeys.COMMANDS_BASE.defaultValue() : root;
    }

    private static String[] readAliases() {
        List<String> aliases = CONFIG.getStringList(ConfigKeys.COMMAND_ALIASES.path());

        if (!aliases.isEmpty()) {
            return aliases.stream()
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);
        }

        return new String[]{ConfigKeys.COMMAND_ALIASES.defaultValue()};
    }
}
