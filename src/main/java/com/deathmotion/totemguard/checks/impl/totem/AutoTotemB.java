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

public final class AutoTotemB extends Check implements Listener {

    private final TotemGuard plugin;
    private final MessageService messageService;

    private final ConcurrentHashMap<UUID, Integer> lowSDCountMap = new ConcurrentHashMap<>();

    public AutoTotemB(TotemGuard plugin) {
        super(plugin, "AutoTotemB", "Impossible stDev");

        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemCycle(TotemCycleEvent event) {
        Player player = event.getPlayer();

        List<Long> intervals = event.getTotemPlayer().totemData().getLatestIntervals(2);
        if (intervals.size() < 2) return;

        double standardDeviation = MathUtil.getStandardDeviation(intervals);
        double mean = MathUtil.getMean(intervals);

        var settings = plugin.getConfigManager().getChecks().getAutoTotemB();
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
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("Standard Deviation: ", colorScheme.getY()))
                .append(Component.text(MathUtil.trim(2, standardDeviation), colorScheme.getX()))
                .append(Component.newline())
                .append(Component.text("Mean: ", colorScheme.getY()))
                .append(Component.text(MathUtil.trim(2, mean) + "ms", colorScheme.getX()))
                .build();
    }

    @Override
    public void resetData() {
        super.resetData();
        lowSDCountMap.clear();
    }

    @Override
    public void resetData(UUID uuid) {
        super.resetData(uuid);
        lowSDCountMap.remove(uuid);
    }
}
