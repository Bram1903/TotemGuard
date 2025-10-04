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

package com.deathmotion.totemguard.commands.cloud.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.cloud.AbstractCommand;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public final class ReloadCommand extends AbstractCommand {

    private final TotemGuard plugin;
    private final CommandMessengerService commandMessengerService;

    public ReloadCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.commandMessengerService = plugin.getMessengerService().getCommandMessengerService();
    }

    @Override
    public void register(final LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(root(commandManager)
                .literal("reload", Description.of("Reloads the plugin"))
                .permission(perm("Reload"))
                .handler(this::handle)
        );
    }

    private void handle(@NonNull final CommandContext<CommandSender> ctx) {
        CommandSender sender = ctx.sender();
        plugin.getConfigManager().reload();
        sender.sendMessage(commandMessengerService.pluginReloaded());
    }
}
