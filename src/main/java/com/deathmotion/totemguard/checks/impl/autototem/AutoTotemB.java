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

package com.deathmotion.totemguard.checks.impl.autototem;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.checks.CheckData;
import com.deathmotion.totemguard.checks.type.BukkitEventCheck;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.deathmotion.totemguard.util.MathUtil;
import net.kyori.adventure.text.Component;

import java.util.List;

@CheckData(name = "AutoTotemB", description = "Suspicious standard deviation")
public class AutoTotemB extends Check implements BukkitEventCheck {

    private int lowStandardDeviationCount = 0;

    public AutoTotemB(TotemPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onTotemCycleEvent() {
        List<Long> intervals = player.totemData.getLatestIntervals(2);
        if (intervals.size() < 2) return;

        double standardDeviation = MathUtil.getStandardDeviation(intervals);
        double mean = MathUtil.getMean(intervals);

        var settings = TotemGuard.getInstance().getConfigManager().getChecks().getAutoTotemB();
        if (standardDeviation >= settings.getStandardDeviationThreshold()) return;
        if (mean >= settings.getMeanThreshold()) return;

        lowStandardDeviationCount = lowStandardDeviationCount + 1;

        if (lowStandardDeviationCount >= settings.getConsecutiveLowSDCount()) {
            lowStandardDeviationCount = 0;
            fail(createComponent(standardDeviation, mean));
        }
    }

    private Component createComponent(double standardDeviation, double mean) {
        return Component.text()
                .append(Component.text("Standard Deviation: ", color.getX()))
                .append(Component.text(MathUtil.trim(2, standardDeviation), color.getY()))
                .append(Component.newline())
                .append(Component.text("Mean: ", color.getX()))
                .append(Component.text(MathUtil.trim(2, mean) + "ms", color.getY()))
                .build();
    }
}
