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
import com.deathmotion.totemguard.data.SafetyStatus;
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.database.entities.impl.Alert;
import com.deathmotion.totemguard.database.entities.impl.Punishment;
import com.deathmotion.totemguard.mojang.MojangService;
import com.deathmotion.totemguard.mojang.models.Callback;
import com.deathmotion.totemguard.util.datastructure.Pair;
import com.deathmotion.totemguard.util.messages.ProfileCreator;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileCommand implements SubCommand {
    private final TotemGuard plugin;
    private final MojangService mojangService;
    private final DatabaseService databaseService;
    private final ZoneId zoneId;

    private final Component noPlayerSpecifiedComponent;
    private final Component loadingComponent;

    public ProfileCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.mojangService = plugin.getMojangService();
        this.databaseService = plugin.getDatabaseService();
        zoneId = ZoneId.systemDefault();

        noPlayerSpecifiedComponent = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                .append(Component.text(" Usage: /totemguard profile <player>", NamedTextColor.RED))
                .build();
        loadingComponent = Component.text()
                .append(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix()))
                .append(Component.text(" Loading profile...", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!validateArgs(sender, args)) {
            return false;
        }

        sender.sendMessage(loadingComponent);
        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> handleAsyncTask(sender, args[1]));

        return true;
    }

    private boolean validateArgs(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(noPlayerSpecifiedComponent);
            return false;
        }
        return true;
    }

    private void handleAsyncTask(CommandSender sender, String playerName) {
        long startTime = System.currentTimeMillis();

        Callback response = mojangService.getUUID(playerName);
        if (!handleApiResponse(sender, response)) {
            return;
        }

        Pair<List<Alert>, List<Punishment>> logs = databaseService.getLogs(response.getUuid());

        List<Alert> alerts = logs.getX();
        List<Punishment> punishments = logs.getY();

        List<Alert> alertsToday = filterAlertsToday(alerts);

        long loadTime = System.currentTimeMillis() - startTime;
        SafetyStatus safetyStatus = SafetyStatus.getSafetyStatus(alertsToday.size(), punishments.size());
        Component logsComponent = ProfileCreator.createProfileComponent(response.getUsername(), alerts, punishments, loadTime, safetyStatus);
        sender.sendMessage(logsComponent);
    }

    private boolean handleApiResponse(CommandSender sender, Callback response) {
        if (response.getUsername() == null) {
            sender.sendMessage(response.getMessage());
            return false;
        }

        return true;
    }

    private List<Alert> filterAlertsToday(List<Alert> alerts) {
        return alerts.stream()
                .filter(alert -> alert.getWhenCreated()
                        .atZone(zoneId)
                        .toLocalDate()
                        .equals(LocalDate.now()))
                .toList();
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
