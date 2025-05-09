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

package com.deathmotion.totemguard.commands.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.commands.CommandSuggestionUtil;
import com.deathmotion.totemguard.commands.OfflinePlayerCommandHandler;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.models.impl.ProfileData;
import com.deathmotion.totemguard.models.impl.SafetyStatus;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncOfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ProfileCommand {

    private final TotemGuard plugin;
    private final DatabaseProvider databaseProvider;
    private final MessengerService messengerService;
    private final CommandMessengerService commandMessengerService;
    private final ZoneId zoneId;

    public ProfileCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseProvider = plugin.getDatabaseProvider();
        this.messengerService = plugin.getMessengerService();
        this.commandMessengerService = messengerService.getCommandMessengerService();
        zoneId = ZoneId.systemDefault();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("profile")
                .withPermission("TotemGuard.Profile")
                .withArguments(new AsyncOfflinePlayerArgument("target").replaceSuggestions(
                        CommandSuggestionUtil.getOfflinePlayerNameSuggestions()
                ))
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        CompletableFuture<OfflinePlayer> target = (CompletableFuture<OfflinePlayer>) args.get("target");
        String rawUsername = args.getRaw("target");
        sender.sendMessage(commandMessengerService.loadingProfile(rawUsername));

        OfflinePlayerCommandHandler.handlePlayerTarget(sender, target, rawUsername, this::handleCommand);
    }

    private void handleCommand(CommandSender sender, OfflinePlayer target, String rawUsername) {
        long startTime = System.currentTimeMillis();

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            ProfileData profileData = databaseProvider.getGenericService().retrieveProfileData(target.getUniqueId());
            if (profileData == null) {
                sender.sendMessage(commandMessengerService.noDatabasePlayerFound(rawUsername));
                return;
            }

            List<DatabaseAlert> alerts = profileData.databaseAlertList();
            List<DatabasePunishment> punishments = profileData.databasePunishmentList();

            List<DatabaseAlert> alertsToday = filterAlertsToday(alerts);

            long loadTime = System.currentTimeMillis() - startTime;
            SafetyStatus safetyStatus = SafetyStatus.getSafetyStatus(alertsToday.size(), punishments.size());

            sender.sendMessage(messengerService.getProfileMessageService().createProfileMessage(rawUsername, profileData.clientBrand(), alerts, punishments, loadTime, safetyStatus));
        });
    }

    private List<DatabaseAlert> filterAlertsToday(List<DatabaseAlert> alerts) {
        return alerts.stream()
                .filter(alert -> alert.getWhenCreated()
                        .atZone(zoneId)
                        .toLocalDate()
                        .equals(LocalDate.now()))
                .toList();
    }
}
