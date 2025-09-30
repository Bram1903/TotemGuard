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
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;


public class PlayerOnlyCommand extends AbstractCommand {

    @Override
    public void register(LegacyPaperCommandManager<CommandSender> commandManager) {
        commandManager.command(
                root(commandManager)
                        .literal("playeronly", Description.of("A command that can only be run by a player"))
                        .permission(perm("playeronly"))
                        .senderType(Player.class)
                        .handler(this::handlePlayerOnlyCommand)
        );
    }

    private void handlePlayerOnlyCommand(@NonNull CommandContext<Player> context) {
        Player player = context.sender();
        player.sendMessage(MessageUtil.getPrefix().append(
                Component.text(" Hello " + player.getName() + "!", NamedTextColor.GREEN)
        ));
    }
}
