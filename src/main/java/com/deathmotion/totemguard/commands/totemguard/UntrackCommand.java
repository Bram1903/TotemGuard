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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class UntrackCommand implements SubCommand {
    private final TotemGuard plugin;
    private final TrackerManager trackerManager;
    private final MessageService messageService;

    public UntrackCommand(TotemGuard plugin) {
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

        if (trackerManager.isTracking(player)) {
            trackerManager.stopTracking(player);
        } else {
            player.sendMessage(getNotTrackingComponent());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    private Component getPlayerOnlyCommandComponent() {
        return messageService.getPrefix().append(Component.text("This command can only be ran by players!", NamedTextColor.RED));
    }

    private Component getNotTrackingComponent() {
        return messageService.getPrefix().append(Component.text("You are not tracking any players!", NamedTextColor.RED));
    }
}