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
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.messenger.impl.StatsMessageService;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.command.CommandSender;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsCommand {

    private final TotemGuard plugin;
    private final DatabaseProvider databaseProvider;
    private final StatsMessageService statsMessageService;

    public StatsCommand(TotemGuard plugin) {
        this.plugin = plugin;
        this.databaseProvider = plugin.getDatabaseProvider();
        this.statsMessageService = plugin.getMessengerService().getStatsMessageService();
    }

    public CommandAPICommand init() {
        return new CommandAPICommand("stats")
                .withPermission("TotemGuard.Stats")
                .executes(this::onCommand);
    }

    private void onCommand(CommandSender sender, CommandArguments args) {
        sender.sendMessage(statsMessageService.statsLoading());

        FoliaScheduler.getAsyncScheduler().runNow(plugin, task -> {
            Instant now = Instant.now();
            Instant dayAgo = now.minus(1, ChronoUnit.DAYS);
            Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
            Instant monthAgo = now.minus(30, ChronoUnit.DAYS);

            ExecutorService dbExec = Executors.newFixedThreadPool(4);

            CompletableFuture<Long> totalPunishments = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getPunishmentRepository().countAllPunishments();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);
            CompletableFuture<Long> punishments30 = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getPunishmentRepository().countPunishmentsSince(monthAgo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);
            CompletableFuture<Long> punishments7 = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getPunishmentRepository().countPunishmentsSince(weekAgo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);
            CompletableFuture<Long> punishments1 = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getPunishmentRepository().countPunishmentsSince(dayAgo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);

            CompletableFuture<Long> totalAlerts = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getAlertRepository().countAllAlerts();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);
            CompletableFuture<Long> alerts30 = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getAlertRepository().countAlertsSince(monthAgo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);
            CompletableFuture<Long> alerts7 = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getAlertRepository().countAlertsSince(weekAgo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);
            CompletableFuture<Long> alerts1 = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseProvider.getAlertRepository().countAlertsSince(dayAgo);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, dbExec);

            // 4) when all done, unbox to int and send
            CompletableFuture
                    .allOf(
                            totalPunishments, punishments30, punishments7, punishments1,
                            totalAlerts, alerts30, alerts7, alerts1
                    )
                    .whenComplete((__, ex) -> {
                        if (ex != null) {
                            sender.sendMessage("§cFailed to load stats: " + ex.getCause().getMessage());
                        } else {
                            // stats(punishmentCount, alertCount, p30, p7, p1, a30, a7, a1)
                            sender.sendMessage(statsMessageService.stats(
                                    totalPunishments.join().intValue(),
                                    totalAlerts.join().intValue(),
                                    punishments30.join().intValue(),
                                    punishments7.join().intValue(),
                                    punishments1.join().intValue(),
                                    alerts30.join().intValue(),
                                    alerts7.join().intValue(),
                                    alerts1.join().intValue()
                            ));
                        }
                        dbExec.shutdown();
                    });
        });
    }
}

