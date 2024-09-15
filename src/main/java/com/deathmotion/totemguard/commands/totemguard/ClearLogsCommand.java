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
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ClearLogsCommand implements SubCommand {
    private final TotemGuard plugin;
    private final Component usageComponent;
    private final Component invalidPlayerComponent;
    private final Component clearingStartedComponent;

    public ClearLogsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        usageComponent = Component.text("Usage: /totemguard clearlogs <player>", NamedTextColor.RED);
        invalidPlayerComponent = Component.text("Player not found!", NamedTextColor.RED);
        clearingStartedComponent = Component.text("Clearing logs...", NamedTextColor.GREEN);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(usageComponent);
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(invalidPlayerComponent);
            return false;
        }

        sender.sendMessage(clearingStartedComponent);
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            long startTime = System.currentTimeMillis();
            int deletedAlerts = plugin.getDatabaseService().clearAlerts(target.getUniqueId());
            int deletedPunishments = plugin.getDatabaseService().clearPunishments(target.getUniqueId());
            int deletedLogs = deletedAlerts + deletedPunishments;
            long loadTime = System.currentTimeMillis() - startTime;

            sender.sendMessage(Component.text("Cleared " + deletedLogs + " logs for" + target.getName() + " in " + loadTime + "ms", NamedTextColor.GREEN));
        });
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
}
