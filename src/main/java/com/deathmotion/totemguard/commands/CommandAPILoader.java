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
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPILogger;

public class CommandAPILoader {

    private TotemGuard plugin;

    public CommandAPILoader(TotemGuard plugin) {
        this.plugin = plugin;
        init();
    }

    public void init() {
        CommandAPI.setLogger(CommandAPILogger.fromJavaLogger(plugin.getLogger()));
        CommandAPIBukkitConfig config = new CommandAPIBukkitConfig(plugin);
        CommandAPI.onLoad(config);
    }

    public void enable() {
        CommandAPI.onEnable();
    }

    public void disable() {
        CommandAPI.onDisable();
    }
}
