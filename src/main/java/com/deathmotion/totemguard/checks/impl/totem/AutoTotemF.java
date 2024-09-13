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

public final class AutoTotemF extends Check implements TotemEventListener {

    private final TotemGuard plugin;

    public AutoTotemF(TotemGuard plugin) {
        super(plugin, "AutoTotemF", "Impossible low outliers", true);
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        List<Long> intervals = totemPlayer.getTotemData().getLatestIntervals(30);
        if (intervals.size() < 30) return;

        List<Double> lowOutliers = MathUtil.getOutliers(intervals).getX();
        double standardDeviation = MathUtil.getStandardDeviation(lowOutliers);
        double mean = MathUtil.getMean(lowOutliers);

        plugin.debug("== AutoTotemF ==");
        plugin.debug("Standard Deviation: " + standardDeviation);
        plugin.debug("Mean: " + mean + "ms");
        plugin.debug("Low Outliers: " + lowOutliers);
    }
}