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

package com.deathmotion.totemguard.commands.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.messenger.impl.StatsMessageService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.command.CommandSender;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class StatsCommand {

    private final TotemGuard plugin;
    private final DatabaseService databaseService;
    private final StatsMessageService statsMessageService;
    private final ZoneId zoneId;

    public StatsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseService = plugin.getDatabaseService();
        this.statsMessageService = plugin.getMessengerService().getStatsMessageService();
        this.zoneId = ZoneId.systemDefault();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("stats")
                .withPermission("TotemGuard.Stats")
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        sender.sendMessage(statsMessageService.statsLoading());

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            List<DatabasePunishment> punishments = databaseService.retrievePunishments();
            List<DatabaseAlert> alerts = databaseService.retrieveAlerts();

            int punishmentCount = punishments.size();
            int alertCount = alerts.size();

            long punishmentsLast30Days = countPunishmentsSince(punishments, 30);
            long punishmentsLast7Days = countPunishmentsSince(punishments, 7);
            long punishmentsLastDay = countPunishmentsSince(punishments, 1);

            long alertsLast30Days = countAlertsSince(alerts, 30);
            long alertsLast7Days = countAlertsSince(alerts, 7);
            long alertsLastDay = countAlertsSince(alerts, 1);

            sender.sendMessage(statsMessageService.stats(punishmentCount, alertCount, punishmentsLast30Days, punishmentsLast7Days, punishmentsLastDay, alertsLast30Days, alertsLast7Days, alertsLastDay));
        });
    }

    private long countPunishmentsSince(List<DatabasePunishment> punishments, int days) {
        LocalDate dateThreshold = LocalDate.now().minusDays(days);
        return punishments.stream()
                .filter(punishment -> punishment.getWhenCreated()
                        .atZone(zoneId)
                        .toLocalDate()
                        .isAfter(dateThreshold))
                .count();
    }

    private long countAlertsSince(List<DatabaseAlert> alerts, int days) {
        LocalDate dateThreshold = LocalDate.now().minusDays(days);
        return alerts.stream()
                .filter(alert -> alert.getWhenCreated()
                        .atZone(zoneId)
                        .toLocalDate()
                        .isAfter(dateThreshold))
                .count();
    }
}
