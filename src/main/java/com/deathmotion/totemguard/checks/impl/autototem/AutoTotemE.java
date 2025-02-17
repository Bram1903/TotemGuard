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

package com.deathmotion.totemguard.checks.impl.autototem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.BukkitEventCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.MathUtil;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@CheckData(name = "AutoTotemE", description = "Suspicious low outliers")
public class AutoTotemE extends Check implements BukkitEventCheck {

    private final ConcurrentLinkedDeque<Double> lowOutliersTracker = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Double> averageStandardDeviation = new ConcurrentLinkedDeque<>();

    public AutoTotemE(TotemPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onTotemCycleEvent() {
        List<Long> intervals = player.totemData.getLatestIntervals(15);
        if (intervals.size() < 4) return;

        // Get the low outliers from the intervals
        List<Double> lowOutliers = MathUtil.getOutliers(intervals).getX();

        // Add the current low outliers to the tracker
        lowOutliersTracker.addAll(lowOutliers);

        // Limit the stored outliers to the last 30 entries
        while (lowOutliersTracker.size() > 30) {
            lowOutliersTracker.poll();
        }

        // Perform calculations if there are enough outliers
        if (lowOutliersTracker.size() >= 15) {
            double standardDeviation = MathUtil.getStandardDeviation(lowOutliersTracker);

            // Store standard deviations for consistency check
            averageStandardDeviation.addLast(standardDeviation);

            // Limit the stored standard deviations to the last 10 entries
            while (averageStandardDeviation.size() > 10) {
                averageStandardDeviation.poll();
            }

            // Calculate average standard deviation to find consistency
            double averageStDeviation = MathUtil.getMean(averageStandardDeviation);

            var settings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemE();

            // Check if both the mean and standard deviation are consistently low
            if (standardDeviation < settings.getStandardDeviationThreshold() && averageStDeviation < settings.getAverageStDeviationThreshold()) {
                fail(createComponent(standardDeviation, averageStDeviation));
            }
        }
    }

    private Component createComponent(double standardDeviation, double averageStDeviation) {
        return Component.text()
                .append(Component.text("Standard Deviation: ", color.getX()))
                .append(Component.text(MathUtil.trim(2, standardDeviation) + "ms", color.getY()))
                .append(Component.newline())
                .append(Component.text("Average Stdev Mean: ", color.getX()))
                .append(Component.text(MathUtil.trim(2, averageStDeviation), color.getY()))
                .build();
    }
}
