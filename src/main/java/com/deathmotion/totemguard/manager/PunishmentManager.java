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
import com.deathmotion.totemguard.data.CheckDetails;
import com.deathmotion.totemguard.data.TotemPlayer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Blocking;

import java.util.concurrent.CountDownLatch;

public class PunishmentManager {
    private final TotemGuard plugin;
    private final DiscordManager discordManager;

    public PunishmentManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.discordManager = plugin.getDiscordManager();
    }

    @Blocking
    public boolean handlePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        if (checkDetails.isPunishable() && checkDetails.getViolations() == checkDetails.getMaxViolations()) {
            long delay = checkDetails.getPunishmentDelay() * 20L;
            if (delay <= 0) {
                executePunishment(totemPlayer, checkDetails);
            } else {
                return scheduleAndWaitPunishment(totemPlayer, checkDetails, delay);
            }
            return true;
        }
        return false;
    }

    private void executePunishment(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        FoliaScheduler.getGlobalRegionScheduler().run(plugin, (o) -> {
            runPunishmentCommands(totemPlayer, checkDetails);
            discordManager.sendPunishment(totemPlayer, checkDetails);
        });
    }

    private boolean scheduleAndWaitPunishment(TotemPlayer totemPlayer, CheckDetails checkDetails, long delay) {
        CountDownLatch latch = new CountDownLatch(1);

        FoliaScheduler.getGlobalRegionScheduler().runDelayed(plugin, (o) -> {
            runPunishmentCommands(totemPlayer, checkDetails);
            discordManager.sendPunishment(totemPlayer, checkDetails);
            latch.countDown(); // Signal that the task is complete
        }, delay);

        try {
            // Wait for the task to complete
            latch.await(); // This can block until latch.countDown() is called
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Handle interrupt
            return false; // Return false if interrupted
        }

        return true; // Only return true once punishment has been executed
    }

    private void runPunishmentCommands(TotemPlayer totemPlayer, CheckDetails checkDetails) {
        String[] punishmentCommands = checkDetails.getPunishmentCommands();
        for (String command : punishmentCommands) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command.replace("%player%", totemPlayer.getUsername()));
        }
    }
}