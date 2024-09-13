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
import com.deathmotion.totemguard.util.datastructure.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemE extends Check implements TotemEventListener {

    private final TotemGuard plugin;

    // Store player-specific previous data using ConcurrentHashMap for thread safety
    private final ConcurrentHashMap<UUID, CheckData> playerDataMap = new ConcurrentHashMap<>();

    public AutoTotemE(TotemGuard plugin) {
        super(plugin, "AutoTotemE", "Impossible consistency", true);
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        UUID playerUUID = player.getUniqueId();
        List<Long> intervals = totemPlayer.getTotemData().getLatestIntervals(15);
        if (intervals.size() < 4) return;

        // Calculate standard deviation, mean, skewness, and outliers using IQR
        double standardDeviation = MathUtil.getStandardDeviation(intervals);
        double mean = MathUtil.getMean(intervals);
        double skewness = MathUtil.getSkewness(intervals);

        // Use the IQR-based method to get both low and high outliers
        Pair<List<Double>, List<Double>> outliers = MathUtil.getOutliers(intervals);
        List<Double> lowOutliers = outliers.getX();  // Low outliers (fast actions)
        List<Double> highOutliers = outliers.getY(); // High outliers (slow actions)

        //plugin.debug("===================");
        //plugin.debug("Player: " + player.getName());
        //plugin.debug("Standard Deviation: " + standardDeviation);
        //plugin.debug("Mean: " + mean);
        //plugin.debug("Skewness: " + skewness);
        //plugin.debug("Low Outliers: " + lowOutliers);
        //plugin.debug("High Outliers: " + highOutliers);

        // Retrieve or create player data
        CheckData data = playerDataMap.computeIfAbsent(playerUUID, uuid -> new CheckData());

        // Store the current analysis for comparison with previous events
        data.addStandardDeviation(standardDeviation);
        data.addSkewness(skewness);
        data.addLowOutliers(lowOutliers);
        data.addHighOutliers(highOutliers);

        // Dynamic thresholds based on standard deviation and mean
        double dynamicLowThreshold = Math.max(50, mean - standardDeviation * 1.5);

        // Add more tolerance to higher deviations for legit players
        if (standardDeviation > 60) {
            dynamicLowThreshold *= 1.2;
        }

        // Adjust detection logic based on more refined thresholds
        if (isSuspiciousBehavior(standardDeviation, skewness, lowOutliers, highOutliers, dynamicLowThreshold)) {
            flag(player, createComponent(standardDeviation, mean, skewness, lowOutliers.size(), highOutliers.size()), plugin.getConfigManager().getSettings().getChecks().getAutoTotemE());
        }

        // Limit the stored data to 10 events for each player
        data.trimToSize(10);
    }

    private boolean isSuspiciousBehavior(double standardDeviation, double skewness, List<Double> lowOutliers, List<Double> highOutliers, double lowThreshold) {
        // Cheaters will have more consistent low outliers and narrow deviations
        boolean lowStdDev = standardDeviation < 40; // Tightened threshold for faster detection
        boolean abnormalSkewness = Math.abs(skewness) > 1.0 || skewness < -1.0; // Stricter skewness threshold

        // Focus on low outliers to detect quick, consistent re-totem actions
        boolean frequentLowOutliers = lowOutliers.size() >= 3 && lowOutliers.stream().allMatch(outlier -> outlier < lowThreshold);

        // Mixed outliers may indicate legitimate play, but frequent low outliers are a strong sign of cheating
        boolean suspiciousLowOutliers = frequentLowOutliers && highOutliers.isEmpty();

        // Combine low standard deviation and outlier detection for more consistent flagging
        return (lowStdDev && abnormalSkewness) || suspiciousLowOutliers;
    }

    private Component createComponent(double sd, double mean, double skewness, int lowOutlierCount, int highOutlierCount) {
        return Component.text()
                .append(Component.text("SD: ", NamedTextColor.GRAY))
                .append(Component.text(sd + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Mean: ", NamedTextColor.GRAY))
                .append(Component.text(mean + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Skewness: ", NamedTextColor.GRAY))
                .append(Component.text(skewness, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Low Outliers: ", NamedTextColor.GRAY))
                .append(Component.text(lowOutlierCount, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("High Outliers: ", NamedTextColor.GRAY))
                .append(Component.text(highOutlierCount, NamedTextColor.GOLD))
                .build();
    }

    @Override
    public void resetData() {
        // Clear all player-specific data
        playerDataMap.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        // Remove specific player data
        playerDataMap.remove(uuid);
    }

    // Helper class to store player-specific data
    private static class CheckData {
        private final List<Double> standardDeviations = new ArrayList<>();
        private final List<Double> skewnesses = new ArrayList<>();
        private final List<List<Double>> lowOutliers = new ArrayList<>();
        private final List<List<Double>> highOutliers = new ArrayList<>();

        public void addStandardDeviation(double sd) {
            standardDeviations.add(sd);
        }

        public void addSkewness(double skew) {
            skewnesses.add(skew);
        }

        public void addLowOutliers(List<Double> outliers) {
            lowOutliers.add(new ArrayList<>(outliers)); // Store a copy to avoid mutation
        }

        public void addHighOutliers(List<Double> outliers) {
            highOutliers.add(new ArrayList<>(outliers)); // Store a copy to avoid mutation
        }

        public void trimToSize(int size) {
            if (standardDeviations.size() > size) {
                standardDeviations.subList(0, standardDeviations.size() - size).clear();
            }
            if (skewnesses.size() > size) {
                skewnesses.subList(0, skewnesses.size() - size).clear();
            }
            if (lowOutliers.size() > size) {
                lowOutliers.subList(0, lowOutliers.size() - size).clear();
            }
            if (highOutliers.size() > size) {
                highOutliers.subList(0, highOutliers.size() - size).clear();
            }
        }
    }
}