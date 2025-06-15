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
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.messenger.CommandMessengerService;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.models.impl.SafetyStatus;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncOfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfileCommand {

    private final TotemGuard plugin;
    private final DatabaseProvider db;
    private final MessengerService messenger;
    private final CommandMessengerService cms;
    private final ZoneId zoneId;

    public ProfileCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseProvider();
        this.messenger = plugin.getMessengerService();
        this.cms = messenger.getCommandMessengerService();
        this.zoneId = ZoneId.systemDefault();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("profile")
                .withPermission("TotemGuard.Profile")
                .withArguments(new AsyncOfflinePlayerArgument("target")
                        .replaceSuggestions(CommandSuggestionUtil.getOfflinePlayerNameSuggestions()))
                .executes(this::onCommand);
    }

    @SuppressWarnings("unchecked")
    private void onCommand(CommandSender sender, CommandArguments args) {
        CompletableFuture<OfflinePlayer> targetF = (CompletableFuture<OfflinePlayer>) args.get("target");
        String rawName = args.getRaw("target");
        sender.sendMessage(cms.loadingProfile(rawName));

        OfflinePlayerCommandHandler.handlePlayerTarget(
                sender, targetF, rawName, this::doProfileLookup
        );
    }

    private void doProfileLookup(CommandSender sender, OfflinePlayer target, String rawName) {
        long start = System.currentTimeMillis();

        FoliaScheduler.getAsyncScheduler().runNow(plugin, task -> {
            try {
                UUID uuid = target.getUniqueId();
                Instant dayStart = LocalDate.now(zoneId)
                        .atStartOfDay(zoneId)
                        .toInstant();

                String brand = db.getPlayerRepository()
                        .findByUuid(uuid)
                        .map(DatabasePlayer::getClientBrand)
                        .orElse("");

                long totalAlerts = db.getAlertRepository().countAlertsForPlayer(uuid);
                long totalPunishments = db.getPunishmentRepository().countPunishmentsForPlayer(uuid);

                long alertsToday = db.getAlertRepository().countAlertsSinceForPlayer(uuid, dayStart);

                List<DatabaseAlert> alerts = db.getAlertRepository().findRecentAlertsForPlayer(uuid, 20);
                List<DatabasePunishment> punishments = db.getPunishmentRepository().findRecentPunishmentsForPlayer(uuid, 20);

                long loadTime = System.currentTimeMillis() - start;
                SafetyStatus status = SafetyStatus.getSafetyStatus(
                        (int) alertsToday,
                        (int) totalPunishments
                );

                sender.sendMessage(messenger
                        .getProfileMessageService()
                        .createProfileMessage(
                                rawName,
                                brand,
                                (int) totalAlerts,
                                (int) totalPunishments,
                                loadTime,
                                status,
                                alerts,
                                punishments
                        )
                );

            } catch (Exception e) {
                sender.sendMessage("§cFailed to load profile: " + e.getMessage());
            }
        });
    }
}