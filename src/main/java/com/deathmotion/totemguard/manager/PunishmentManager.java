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

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.events.PunishEvent;
import com.deathmotion.totemguard.api.models.TotemPlayer;
import com.deathmotion.totemguard.database.DatabaseService;
import com.deathmotion.totemguard.models.checks.CheckDetails;
import com.deathmotion.totemguard.util.PlaceholderUtil;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class PunishmentManager {
    private final TotemGuard plugin;
    private final DiscordManager discordManager;
    private final DatabaseService databaseService;

    private final Set<UUID> toBePunished;

    public PunishmentManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.discordManager = plugin.getDiscordManager();
        this.databaseService = plugin.getDatabaseService();

        this.toBePunished = ConcurrentHashMap.newKeySet();
    }

    @Blocking
    public boolean handlePunishment(Player player, TotemPlayer totemPlayer, CheckDetails checkDetails, String prefix) {
        if (checkDetails.isPunishable() && checkDetails.getViolations() >= checkDetails.getMaxViolations() && !toBePunished.contains(totemPlayer.uuid())) {
            PunishEvent punishEvent = new PunishEvent(player, checkDetails);
            plugin.getServer().getPluginManager().callEvent(punishEvent);
            if (punishEvent.isCancelled()) return false;

            toBePunished.add(totemPlayer.uuid());

            long delayTicks = checkDetails.getPunishmentDelay() * 20L;
            if (delayTicks <= 0) {
                executePunishment(totemPlayer, checkDetails, prefix);
            } else {
                return scheduleAndWaitPunishment(totemPlayer, checkDetails, prefix, delayTicks);
            }
            return true;
        }
        return false;
    }

    private void executePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails, String prefix) {
        FoliaScheduler.getGlobalRegionScheduler().run(plugin, (o) -> {
            databaseService.savePunishment(totemPlayer, checkDetails);
            runPunishmentCommands(totemPlayer, checkDetails, prefix);
            discordManager.sendPunishment(totemPlayer, checkDetails);

            toBePunished.remove(totemPlayer.uuid());
        });
    }

    private boolean scheduleAndWaitPunishment(TotemPlayer totemPlayer, CheckDetails checkDetails, String prefix, long delayTicks) {
        CountDownLatch latch = new CountDownLatch(1);

        FoliaScheduler.getGlobalRegionScheduler().runDelayed(plugin, (o) -> {
            databaseService.savePunishment(totemPlayer, checkDetails);
            runPunishmentCommands(totemPlayer, checkDetails, prefix);
            discordManager.sendPunishment(totemPlayer, checkDetails);

            toBePunished.remove(totemPlayer.uuid());
            latch.countDown();
        }, delayTicks);

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return true;
    }

    private void runPunishmentCommands(TotemPlayer totemPlayer, CheckDetails checkDetails, String prefix) {
        // Map of placeholders
        Map<String, String> placeholders = Map.of(
                "%prefix%", prefix,
                "%uuid%", totemPlayer.uuid().toString(),
                "%player%", totemPlayer.username(),
                "%check%", checkDetails.getCheckName(),
                "%description%", checkDetails.getCheckDescription(),
                "%ping%", String.valueOf(checkDetails.getPing()),
                "%tps%", String.valueOf(checkDetails.getTps()),
                "%punishable%", String.valueOf(checkDetails.isPunishable()),
                "%violations%", String.valueOf(checkDetails.getViolations()),
                "%max_violations%", checkDetails.isPunishable() ? String.valueOf(checkDetails.getMaxViolations()) : "∞"
        );

        // Parse the defaultPunishment command
        String defaultPunishmentCommand = PlaceholderUtil.replacePlaceholders(plugin.getConfigManager().getSettings().getChecks().getDefaultPunishment(), placeholders);

        List<String> punishmentCommands = checkDetails.getPunishmentCommands().stream()
                .map(command -> command.replace("%default_punishment%", defaultPunishmentCommand))
                .toList();

        for (String command : punishmentCommands) {
            String parsedCommand = PlaceholderUtil.replacePlaceholders(command, placeholders);
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parsedCommand);
        }
    }
}
