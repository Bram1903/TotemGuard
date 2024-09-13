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
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class AutoTotemF extends Check implements TotemEventListener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedDeque<Double>> lowOutliersTracker = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> eventCountTracker = new ConcurrentHashMap<>();

    public AutoTotemF(TotemGuard plugin) {
        super(plugin, "AutoTotemF", "Impossible low outliers", true);
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        UUID playerId = player.getUniqueId();
        List<Long> intervals = totemPlayer.getTotemData().getLatestIntervals(5);
        if (intervals.size() < 5) return;

        int currentEvent = this.eventCountTracker.getOrDefault(player.getUniqueId(), 0) + 1;
        this.eventCountTracker.put(player.getUniqueId(), currentEvent);
        if (currentEvent <= 5) return;

        eventCountTracker.put(playerId, 0);

        // Get the low outliers from the intervals
        List<Double> lowOutliers = MathUtil.getOutliers(intervals).getX();
        plugin.debug("== AutoTotemF ==");
        plugin.debug("Low Outliers: " + lowOutliers);

        // Add the current low outliers to the tracker
        lowOutliersTracker.computeIfAbsent(playerId, k -> new ConcurrentLinkedDeque<>()).addAll(lowOutliers);

        ConcurrentLinkedDeque<Double> allOutliers = lowOutliersTracker.get(playerId);
        while (allOutliers.size() > 10) {
            allOutliers.pollFirst();
        }

        // Perform calculations if the size is exactly 30
        if (allOutliers.size() == 10) {
            double standardDeviation = MathUtil.getStandardDeviation(allOutliers);
            double mean = MathUtil.getMean(allOutliers);

            plugin.debug("== AutoTotemF (15 Outliers) ==");
            plugin.debug("Standard Deviation: " + MathUtil.trim(2, standardDeviation));
            plugin.debug("Mean: " + MathUtil.trim(2, mean) + "ms");
        } else {
            plugin.debug("== AutoTotemF ==");
            plugin.debug("Added low outliers for player " + player.getName() + ". Current outliers count: " + allOutliers.size());
        }
    }
}
