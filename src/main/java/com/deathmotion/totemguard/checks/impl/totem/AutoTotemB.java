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

public final class AutoTotemB extends Check implements TotemEventListener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, Integer> lowSDCountMap = new ConcurrentHashMap<>();

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Impossible consistency", true);
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        var settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemB();

        // Fetch a larger sample of recent intervals (e.g., 8 or more) for better analysis.
        List<Long> recentIntervals = totemPlayer.getTotemData().getLatestIntervals(8);
        if (recentIntervals.size() < 5) return;

        // Remove outliers to improve accuracy
        recentIntervals = MathUtil.removeOutliers(recentIntervals);

        // Calculate mean, median, and standard deviation
        double standardDeviation = MathUtil.trim(2, MathUtil.getStandardDeviation(recentIntervals));
        double mean = MathUtil.getMean(recentIntervals);
        double median = MathUtil.getMedian(recentIntervals.stream().map(Long::doubleValue).toList());

        // Use rolling windows to look for inconsistencies
        List<Long> recentSubset = recentIntervals.subList(Math.max(recentIntervals.size() - 5, 0), recentIntervals.size());
        double recentSD = MathUtil.getStandardDeviation(recentSubset);
        double recentMean = MathUtil.getMean(recentSubset);

        plugin.debug(player.getName() + " - Recent intervals (filtered): " + recentIntervals);
        plugin.debug(player.getName() + " - Standard deviation: " + standardDeviation);
        plugin.debug(player.getName() + " - Mean: " + mean + ", Median: " + median);

        plugin.debug(player.getName() + " - Recent subset intervals: " + recentSubset);
        plugin.debug(player.getName() + " - Recent subset SD: " + recentSD + ", Mean: " + recentMean);

        int consecutiveLowSDCount = lowSDCountMap.getOrDefault(player.getUniqueId(), 0);

        // Dynamic threshold based on recent behavior
        double adjustedSDThreshold = settings.getLowSDThreshold() * (1 + (consecutiveLowSDCount * 0.1));

        if (standardDeviation >= adjustedSDThreshold && recentSD >= adjustedSDThreshold) {
            // Gradually decrease violation counts instead of decrementing by 1
            consecutiveLowSDCount = Math.max(0, consecutiveLowSDCount - 1);
            lowSDCountMap.put(player.getUniqueId(), consecutiveLowSDCount);
            return;
        }

        // Penalty for mixed consistency: flag if there's a large discrepancy between recent SD and overall SD
        double difference = MathUtil.differenceBetween(standardDeviation, recentSD);
        plugin.debug(player.getName() + " - Difference between SDs: " + difference);

        if (difference > 3) {
            plugin.debug(player.getName() + " - Mixed consistency detected, adding penalty.");
            consecutiveLowSDCount++;
        }

        // Increment consecutive low SD count if suspicious
        consecutiveLowSDCount++;
        lowSDCountMap.put(player.getUniqueId(), consecutiveLowSDCount);
        plugin.debug(player.getName() + " - Consecutive low SD count: " + consecutiveLowSDCount);

        // Flagging the player after low SD count exceeds the threshold
        if (consecutiveLowSDCount >= 5) {
            lowSDCountMap.remove(player.getUniqueId());
            flag(player, createComponent(standardDeviation, mean, median, recentSD, recentMean), settings);
        }
    }

    private Component createComponent(double sd, double mean, double median, double recentSD, double recentMean) {
        return Component.text()
                .append(Component.text("SD" + ": ", NamedTextColor.GRAY))
                .append(Component.text(sd + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Mean" + ": ", NamedTextColor.GRAY))
                .append(Component.text(mean + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Median" + ": ", NamedTextColor.GRAY))
                .append(Component.text(median + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Recent SD" + ": ", NamedTextColor.GRAY))
                .append(Component.text(recentSD + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Recent Mean" + ": ", NamedTextColor.GRAY))
                .append(Component.text(recentMean + "ms", NamedTextColor.GOLD))
                .build();
    }

    @Override
    public void resetData() {
        lowSDCountMap.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        lowSDCountMap.remove(uuid);
    }
}
