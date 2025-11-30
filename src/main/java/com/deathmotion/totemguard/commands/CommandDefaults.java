/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.commands;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.manager.ConfigManager;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.util.Locale;

public final class CommandDefaults {
    private static final ConfigManager CONFIG = TotemGuard.getInstance().getConfigManager();

    public static final String ROOT = CONFIG.getSettings().getCommand();
    public static final String[] ALIASES = CONFIG.getSettings().getCommandAliases().toArray(new String[0]);
    public static final String PERMISSION_PREFIX = "totemguard.";

    private CommandDefaults() {
        // prevent instantiation
    }

    public static Command.Builder<CommandSender> root(final LegacyPaperCommandManager<CommandSender> manager) {
        return manager.commandBuilder(ROOT, Description.of("The root command for TotemGuard."), ALIASES);
    }

    /**
     * Normalizes and prefixes permission nodes, e.g. perm("hello") -> "totemguard.hello"
     */
    public static String perm(final String node) {
        String n = node.trim()
                .toLowerCase(Locale.ROOT)
                .replace(' ', '.')
                .replace("..", ".");
        if (n.startsWith(".")) {
            n = n.substring(1);
        }
        return PERMISSION_PREFIX + n;
    }
}
