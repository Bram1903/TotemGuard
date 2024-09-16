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
import com.deathmotion.totemguard.mojang.ApiResponse;
import com.deathmotion.totemguard.mojang.MojangAPIService;
import com.deathmotion.totemguard.mojang.models.BadRequest;
import com.deathmotion.totemguard.mojang.models.Found;
import com.deathmotion.totemguard.util.messages.ProfileCreator;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProfileCommand implements SubCommand {
    private final TotemGuard plugin;
    private final MojangAPIService mojangAPIService;
    private final DatabaseService databaseService;
    private final ZoneId zoneId;

    private final Component noPlayerSpecifiedComponent;
    private final Component loadingComponent;

    public ProfileCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.mojangAPIService = plugin.getMojangAPIService();
        this.databaseService = plugin.getDatabaseService();
        zoneId = ZoneId.systemDefault();

        noPlayerSpecifiedComponent = Component.text("Usage: /totemguard profile <player>", NamedTextColor.RED);
        loadingComponent = Component.text("Loading profile...", NamedTextColor.GRAY);
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

        ApiResponse response = mojangAPIService.getUUID(playerName);
        if (!handleApiResponse(sender, response)) {
            return;
        }

        Found found = (Found) response;
        UUID uuid = found.uuid();

        List<Alert> alerts = databaseService.getAlerts(uuid);
        List<Punishment> punishments = databaseService.getPunishments(uuid);

        List<Alert> alertsToday = filterAlertsToday(alerts);

        long loadTime = System.currentTimeMillis() - startTime;
        SafetyStatus safetyStatus = SafetyStatus.getSafetyStatus(alertsToday.size(), punishments.size());
        Component logsComponent = ProfileCreator.createProfileComponent(found.username(), alerts, punishments, loadTime, safetyStatus);
        sender.sendMessage(logsComponent);
    }

    private boolean handleApiResponse(CommandSender sender, ApiResponse response) {
        if (response == null) {
            sender.sendMessage(Component.text("An error occurred while fetching the player's UUID.", NamedTextColor.RED));
            return false;
        }

        return switch (response.responseStatus()) {
            case 204 -> {
                sender.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                yield false;
            }
            case 400 -> {
                sender.sendMessage(Component.text(((BadRequest) response).errorMessage(), NamedTextColor.RED));
                yield false;
            }
            case 429 -> {
                sender.sendMessage(Component.text("Rate limit exceeded. Please try again later.", NamedTextColor.RED));
                yield false;
            }
            default -> true;
        };
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
