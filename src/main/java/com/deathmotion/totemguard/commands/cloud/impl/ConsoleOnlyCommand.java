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

import com.deathmotion.totemguard.commands.cloud.AbstractCommand;
import com.deathmotion.totemguard.util.MessageUtil;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class ConsoleOnlyCommand extends AbstractCommand {

    @Override
    public void register(LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(
                root(commandManager)
                        .literal("consoleonly", Description.of("A command that can only be run by the console"))
                        .permission(perm("consoleonly"))
                        .senderType(ConsoleCommandSender.class)
                        .handler(this::handleConsoleOnlyCommand)
        );
    }

    private void handleConsoleOnlyCommand(@NonNull CommandContext<ConsoleCommandSender> context) {
        ConsoleCommandSender console = context.sender();
        console.sendMessage(MessageUtil.getPrefix().append(
                Component.text(" Hello console!", NamedTextColor.GREEN)
        ));
    }
}