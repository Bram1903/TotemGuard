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
import com.deathmotion.totemguard.config.Checks;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.MathUtil;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@CheckData(name = "AutoTotemC", description = "Suspicious average standard deviation")
public class AutoTotemC extends Check implements BukkitEventCheck {

    private final ConcurrentLinkedDeque<Double> standardDeviations = new ConcurrentLinkedDeque<>();
    private int consistentStandardDeviationCount = 0;

    public AutoTotemC(TotemPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onTotemCycleEvent() {
        // Get the player's SD history or create a new one

        List<Long> recentIntervals = player.totemData.getLatestIntervals(4);
        double standardDeviation = MathUtil.getStandardDeviation(recentIntervals);

        // Add the current SD to the history
        standardDeviations.addLast(standardDeviation);
        while (standardDeviations.size() > 4) {
            standardDeviations.pollFirst();
        }

        // Only proceed if we have at least three SDs to compare
        if (standardDeviations.size() >= 2) {
            List<Double> sdList = new ArrayList<>(standardDeviations);
            List<Double> differences = new ArrayList<>();

            // Calculate differences between consecutive SD values
            for (int i = 1; i < sdList.size(); i++) {
                differences.add(Math.abs(sdList.get(i) - sdList.get(i - 1)));
            }

            double averageSDDifference = MathUtil.getMean(differences);
            //plugin.debug(player.getName() + " - Average SD Difference: " + averageSDDifference + "ms");

            Checks.AutoTotemC settings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemC();

            // Check if the average SD difference is below the threshold
            if (averageSDDifference < settings.getConsistentSDRange()) {
                consistentStandardDeviationCount = consistentStandardDeviationCount + 1;

                if (consistentStandardDeviationCount >= settings.getConsecutiveViolations()) {
                    standardDeviations.clear();
                    consistentStandardDeviationCount = 0;
                    fail(createComponent(averageSDDifference));
                }
            } else {
                // Reset the count if the average SD difference is above the range
                consistentStandardDeviationCount = 0;
            }
        }
    }

    private Component createComponent(double averageSDDifference) {
        return Component.text()
                .append(Component.text("Average SD Difference: ", color.getX()))
                .append(Component.text(MathUtil.trim(2, averageSDDifference), color.getY()))
                .build();
    }
}
