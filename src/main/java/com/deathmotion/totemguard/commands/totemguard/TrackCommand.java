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
import com.deathmotion.totemguard.commands.SubCommand;
import com.deathmotion.totemguard.manager.TrackerManager;
import com.deathmotion.totemguard.util.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TrackCommand implements SubCommand {
    private final TotemGuard plugin;
    private final TrackerManager trackerManager;
    private final MessageService messageService;

    public TrackCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.trackerManager = plugin.getTrackerManager();
        this.messageService = plugin.getMessageService();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(getPlayerOnlyCommandComponent());
            return false;
        }

        if (args.length != 2 && trackerManager.isTracking(player)) {
            trackerManager.stopTracking(player);
            return true;
        } else if (args.length != 2) {
            sender.sendMessage(getUsageComponent());
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(getPlayerNotFoundComponent());
            return false;
        }

        trackerManager.startTracking(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String argsLowerCase = args[1].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private Component getUsageComponent() {
        return messageService.getPrefix()
                .append(Component.text("Usage: /totemguard track <player>", NamedTextColor.RED));
    }

    private Component getPlayerNotFoundComponent() {
        return messageService.getPrefix()
                .append(Component.text("Player not found!", NamedTextColor.RED));
    }

    private Component getPlayerOnlyCommandComponent() {
        return messageService.getPrefix()
                .append(Component.text("This command can only be ran by players!", NamedTextColor.RED));
    }
}