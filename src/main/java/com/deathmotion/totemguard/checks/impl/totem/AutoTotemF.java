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
import com.deathmotion.totemguard.checks.TotemEventListener;
import com.deathmotion.totemguard.checks.impl.totem.processor.TotemProcessor;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.util.MathUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class AutoTotemF extends Check implements TotemEventListener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> lowOutliersTracker = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> averageStDev = new ConcurrentHashMap<>();

    public AutoTotemF(TotemGuard plugin) {
        super(plugin, "AutoTotemF", "Impossible low outliers", true);
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        UUID playerId = player.getUniqueId();
        List<Long> intervals = totemPlayer.getTotemData().getLatestIntervals(15);
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
            plugin.debug("== AutoTotemF (Consistency Check) ==");
            plugin.debug("Standard Deviation: " + MathUtil.trim(2, standardDeviation));
            plugin.debug("Average Standard Deviation: " + MathUtil.trim(2, averageStDeviation) + "ms");

            // Check if both the mean and standard deviation are consistently low
            if (standardDeviation < 2.0 && averageStDeviation < 2.0) {
                flag(player, createComponent(standardDeviation, averageStDeviation), plugin.getConfigManager().getSettings().getChecks().getAutoTotemF());
            }
        } else {
            plugin.debug("== AutoTotemF ==");
            plugin.debug("Added low outliers for player " + player.getName() + ". Current outliers count: " + playerOutliers.size());
        }
    }

    private Component createComponent(double standardDeviation, double averageStDeviation) {
        return Component.text()
                .append(Component.text("Standard Deviation: ", NamedTextColor.GRAY))
                .append(Component.text(MathUtil.trim(2, standardDeviation) + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Average Stdev Mean: ", NamedTextColor.GRAY))
                .append(Component.text(MathUtil.trim(2, averageStDeviation), NamedTextColor.GOLD))
                .build();
    }

    @Override
    public void resetData() {
        lowOutliersTracker.clear();
        averageStDev.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        lowOutliersTracker.remove(uuid);
        averageStDev.remove(uuid);
    }
}
