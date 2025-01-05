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

package com.deathmotion.totemguard.commands.totemguard;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.messenger.impl.CommandMessengerService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;

public class ReloadCommand {
    private final TotemGuard plugin;
    private final CommandMessengerService commandMessengerService;

    public ReloadCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("reload")
                .withPermission("TotemGuard.Reload")
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        plugin.getConfigManager().reload();
        sender.sendMessage(commandMessengerService.pluginReloaded());
    }
}
