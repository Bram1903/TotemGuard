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
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.messenger.impl.CommandMessengerService;
import com.deathmotion.totemguard.models.impl.SafetyStatus;
import com.deathmotion.totemguard.util.datastructure.Pair;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.AsyncOfflinePlayerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ProfileCommand {

    private final TotemGuard plugin;
    private final DatabaseService databaseService;
    private final MessengerService messengerService;
    private final CommandMessengerService commandMessengerService;
    private final ZoneId zoneId;

    public ProfileCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseService = plugin.getDatabaseService();
        this.messengerService = plugin.getMessengerService();
        this.commandMessengerService = messengerService.getCommandMessengerService();
        zoneId = ZoneId.systemDefault();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("profile")
                .withPermission("TotemGuard.Profile")
                .withArguments(new AsyncOfflinePlayerArgument("target").replaceSuggestions(
                        ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() ->
                                Stream.of(Bukkit.getOfflinePlayers())
                                        .map(OfflinePlayer::getName)
                                        .toArray(String[]::new)
                        ))
                ))
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        CompletableFuture<OfflinePlayer> target = (CompletableFuture<OfflinePlayer>) args.get("target");

        sender.sendMessage(commandMessengerService.loadingProfile(args.getRaw("target")));
        target.thenAccept(offlinePlayer -> {
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                sender.sendMessage(commandMessengerService.targetNeverJoined());
                return;
            }

            handleCommand(sender, offlinePlayer);
        }).exceptionally(throwable -> {
            Throwable cause = throwable.getCause();
            Throwable rootCause = cause instanceof RuntimeException ? cause.getCause() : cause;

            sender.sendMessage(Component.text(rootCause.getMessage(), NamedTextColor.RED));
            return null;
        });
    }

    private void handleCommand(CommandSender sender, OfflinePlayer target) {
        long startTime = System.currentTimeMillis();

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            Pair<List<DatabaseAlert>, List<DatabasePunishment>> logs = databaseService.retrieveLogs(target.getUniqueId());
            if (logs == null) {
                sender.sendMessage(commandMessengerService.noDatabasePlayerFound(target.getName()));
                return;
            }

            List<DatabaseAlert> alerts = logs.getX();
            List<DatabasePunishment> punishments = logs.getY();

            List<DatabaseAlert> alertsToday = filterAlertsToday(alerts);

            long loadTime = System.currentTimeMillis() - startTime;
            SafetyStatus safetyStatus = SafetyStatus.getSafetyStatus(alertsToday.size(), punishments.size());

            sender.sendMessage(messengerService.getProfileMessageService().createProfileMessage(target.getName(), alerts, punishments, loadTime, safetyStatus));
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
