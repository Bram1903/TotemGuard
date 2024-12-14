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

package com.deathmotion.totemguard.checks.impl.totem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.models.events.TotemCycleEvent;
import com.deathmotion.totemguard.util.MathUtil;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class AutoTotemE extends Check implements Listener {

    private final TotemGuard plugin;
    private final MessageService messageService;

    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> lowOutliersTracker = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> averageStDev = new ConcurrentHashMap<>();

    public AutoTotemE(TotemGuard plugin) {
        super(plugin, "AutoTotemE", "Impossible low outliers", true);
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemCycle(TotemCycleEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        List<Long> intervals = event.getTotemPlayer().totemData().getLatestIntervals(15);
        if (intervals.size() < 4) return;

        // Get the low outliers from the intervals
        List<Double> lowOutliers = MathUtil.getOutliers(intervals).getX();

        // Add the current low outliers to the tracker
        ConcurrentLinkedDeque<Double> playerOutliers = lowOutliersTracker.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());
        playerOutliers.addAll(lowOutliers);

        // Limit the stored outliers to the last 30 entries
        while (playerOutliers.size() > 30) {
            playerOutliers.poll();
        }

        // Perform calculations if there are enough outliers
        if (playerOutliers.size() >= 15) {
            double standardDeviation = MathUtil.getStandardDeviation(playerOutliers);

            // Store standard deviations for consistency check
            ConcurrentLinkedDeque<Double> stDevHistory = averageStDev.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>());
            stDevHistory.addLast(standardDeviation);

            // Limit the stored standard deviations to the last 10 entries
            while (stDevHistory.size() > 10) {
                stDevHistory.poll();
            }

            // Calculate average standard deviation to find consistency
            double averageStDeviation = MathUtil.getMean(stDevHistory);

            var settings = plugin.getConfigManager().getChecks().getAutoTotemE();

            // Check if both the mean and standard deviation are consistently low
            if (standardDeviation < settings.getStandardDeviationThreshold() && averageStDeviation < settings.getAverageStDeviationThreshold()) {
                flag(player, createComponent(standardDeviation, averageStDeviation), settings);
            }
        }
    }

    private Component createComponent(double standardDeviation, double averageStDeviation) {
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("Standard Deviation: ", colorScheme.getY()))
                .append(Component.text(MathUtil.trim(2, standardDeviation) + "ms", colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Average Stdev Mean: ", colorScheme.getY()))
                .append(Component.text(MathUtil.trim(2, averageStDeviation), colorScheme.getX()))
                .build();
    }

    @Override
    public void resetData() {
        super.resetData();
        lowOutliersTracker.clear();
        averageStDev.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
        lowOutliersTracker.remove(uuid);
        averageStDev.remove(uuid);
    }
}
