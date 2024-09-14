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
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import com.deathmotion.totemguard.util.AlertCreator;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class LogsCommand implements SubCommand {
    private final TotemGuard plugin;
    private final DatabaseService alertService;

    public LogsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.alertService = plugin.getDatabaseService();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /totemguard logs <player>", NamedTextColor.RED));
            return false;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
            return false;
        }

        sender.sendMessage(Component.text("Retrieving database logs..", NamedTextColor.WHITE));
        long startTime = System.currentTimeMillis();

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            List<Alert> alerts = alertService.getAlerts(target.getUniqueId());
            List<Punishment> punishments = alertService.getPunishments(target.getUniqueId());

            long loadTime = System.currentTimeMillis() - startTime;
            Component logsComponent = AlertCreator.createLogsComponent(target, alerts, punishments, loadTime);
            sender.sendMessage(logsComponent);
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2 && sender.hasPermission("TotemGuard.Logs")) {
            String argsLowerCase = args[1].toLowerCase();

            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(argsLowerCase))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
