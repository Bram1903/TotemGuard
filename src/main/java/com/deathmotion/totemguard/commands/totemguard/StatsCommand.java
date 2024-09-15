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
import com.deathmotion.totemguard.util.messages.StatsCreator;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class StatsCommand implements SubCommand {
    private final Plugin plugin;
    private final DatabaseService databaseService;
    private final ZoneId zoneId;
    private final Component loadingComponent;

    public StatsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseService = plugin.getDatabaseService();

        this.zoneId = ZoneId.systemDefault();
        this.loadingComponent = Component.text("Loading stats...", NamedTextColor.GRAY);
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage(loadingComponent);

        FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
            List<Punishment> punishments = databaseService.getPunishments();
            List<Alert> alerts = databaseService.getAlerts();

            int punishmentCount = punishments.size();
            int alertCount = alerts.size();

            long punishmentsLast30Days = countPunishmentsSince(punishments, 30);
            long punishmentsLast7Days = countPunishmentsSince(punishments, 7);
            long punishmentsLastDay = countPunishmentsSince(punishments, 1);

            long alertsLast30Days = countAlertsSince(alerts, 30);
            long alertsLast7Days = countAlertsSince(alerts, 7);
            long alertsLastDay = countAlertsSince(alerts, 1);

            Component stats = StatsCreator.createStatsComponent(punishmentCount, alertCount, punishmentsLast30Days, punishmentsLast7Days, punishmentsLastDay, alertsLast30Days, alertsLast7Days, alertsLastDay);
            sender.sendMessage(stats);
        });

        return true;
    }

    private long countPunishmentsSince(List<Punishment> punishments, int days) {
        LocalDate dateThreshold = LocalDate.now().minusDays(days);
        return punishments.stream()
                .filter(punishment -> punishment.getWhenCreated()
                        .atZone(zoneId)
                        .toLocalDate()
                        .isAfter(dateThreshold))
                .count();
    }

    private long countAlertsSince(List<Alert> alerts, int days) {
        LocalDate dateThreshold = LocalDate.now().minusDays(days);
        return alerts.stream()
                .filter(alert -> alert.getWhenCreated()
                        .atZone(zoneId)
                        .toLocalDate()
                        .isAfter(dateThreshold))
                .count();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}