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

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AutoTotemE extends Check implements TotemEventListener {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<UUID, Integer> lowSDCountMap = new ConcurrentHashMap<>();

    public AutoTotemE(TotemGuard plugin) {
        super(plugin, "AutoTotemE", "Impossible re-totem speed without outliers", true);
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        var settings = plugin.getConfigManager().getSettings().getChecks().getAutoTotemE();

        List<Long> recentIntervals = totemPlayer.getLatestIntervals(20);

        if (recentIntervals.size() < 4) return;

        Collection<Long> mellowedIntervals = MathUtil.removeOutliers(recentIntervals);
        double modifiedDeviation = MathUtil.trim(2, MathUtil.getStandardDeviation(mellowedIntervals));

        if (mellowedIntervals.size() < 5 || modifiedDeviation >= settings.getLowSDThreshold()) {
            return;
        }

        int consecutiveLowSDCount = lowSDCountMap.getOrDefault(player.getUniqueId(), 0) + 1;
        lowSDCountMap.put(player.getUniqueId(), consecutiveLowSDCount);

        if (consecutiveLowSDCount >= 1) {
            lowSDCountMap.remove(player.getUniqueId());
            flag(player, createComponent(modifiedDeviation, totemPlayer.getLatestStandardDeviation(), recentIntervals.size(), mellowedIntervals.size()), settings);
        }
    }

    private Component createComponent(double modifiedSd, double originalSd, int intervalCount, int modifiedIntervalCount) {
        return Component.text()
                .append(Component.text("Original SD" + ": ", NamedTextColor.GRAY))
                .append(Component.text(originalSd + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Modified SD" + ": ", NamedTextColor.GRAY))
                .append(Component.text(modifiedSd + "ms", NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Amount of intervals" + ": ", NamedTextColor.GRAY))
                .append(Component.text(intervalCount, NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Amount of modified intervals" + ": ", NamedTextColor.GRAY))
                .append(Component.text(modifiedIntervalCount, NamedTextColor.GOLD))
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
