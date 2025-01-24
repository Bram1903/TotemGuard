/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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
import com.deathmotion.totemguard.commands.impl.*;
import dev.jorel.commandapi.CommandAPICommand;

public class TotemGuardCommand {

    private final TotemGuard plugin;

    public TotemGuardCommand(TotemGuard plugin) {
        this.plugin = plugin;
        init();
    }

    public void init() {
        new CommandAPICommand("totemguard")
                .withAliases("tg")
                .withSubcommands(
                        new ReloadCommand(plugin).init(),
                        new CheckCommand(plugin).init(),
                        new AlertsCommand(plugin).init(),
                        new ProfileCommand(plugin).init(),
                        new DatabaseCommand(plugin).init(),
                        new ClearLogsCommand(plugin).init(),
                        new StatsCommand(plugin).init()
                )
                .executes((sender, args) -> {
                    sender.sendMessage(plugin.getMessengerService().totemGuardInfo());
                })
                .register();
    }
}
