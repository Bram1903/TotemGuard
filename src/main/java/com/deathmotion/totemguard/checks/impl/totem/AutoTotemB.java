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
        super(plugin, "AutoTotemB", "Impossible stDev", true);

        this.plugin = plugin;
        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        List<Long> intervals = totemPlayer.getTotemData().getLatestIntervals(2);
        if (intervals.size() < 2) return;

        double standardDeviation = MathUtil.getStandardDeviation(intervals);
        double mean = MathUtil.getMean(intervals);

        //plugin.debug("== AutoTotemB ==");
        //plugin.debug("Standard Deviation: " + standardDeviation + "ms");
        //plugin.debug("Mean: " + mean + "ms");

        var settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemB();
        if (standardDeviation >= settings.getStandardDeviationThreshold()) return;
        if (mean >= settings.getMeanThreshold()) return;

        int consecutiveLowSDCount = this.lowSDCountMap.getOrDefault(player.getUniqueId(), 0) + 1;
        lowSDCountMap.put(player.getUniqueId(), consecutiveLowSDCount);

        if (consecutiveLowSDCount >= settings.getConsecutiveLowSDCount()) {
            lowSDCountMap.remove(player.getUniqueId());
            flag(player, createComponent(standardDeviation, mean), settings);
        }
    }

    private Component createComponent(double standardDeviation, double mean) {
        return Component.text()
                .append(Component.text("Standard Deviation: ", NamedTextColor.GRAY))
                .append(Component.text(MathUtil.trim(2, standardDeviation), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Mean: ", NamedTextColor.GRAY))
                .append(Component.text(MathUtil.trim(2, mean) + "ms", NamedTextColor.GOLD))
                .build();
    }
}
