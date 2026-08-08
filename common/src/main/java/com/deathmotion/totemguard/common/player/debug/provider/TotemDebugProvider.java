/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.common.player.debug.provider;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.TotemData;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayFrame;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayProvider;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.util.MathUtil;
import com.deathmotion.totemguard.common.util.Palette;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;
import java.util.Locale;

public final class TotemDebugProvider implements DebugOverlayProvider {

    private static Component label(String text) {
        return Component.text(text, Palette.LABEL);
    }

    private static Component value(String text, TextColor color) {
        return Component.text(text, color);
    }

    private static Component separator() {
        return Component.text(" | ", Palette.SEPARATOR);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public String getKey() {
        return "totem";
    }

    @Override
    public String getDisplayName() {
        return "Totem";
    }

    @Override
    public DebugOverlayFrame buildFrame(TGPlayer player) {
        List<Long> intervals = player.getTotemData().getIntervals();
        Long lastUse = player.getLastTotemUse();
        boolean offhandTotem = player.getInventory().isTotemInSlot(InventoryConstants.SLOT_OFFHAND);

        String state;
        TextColor stateColor;
        if (lastUse != null) {
            long age = System.currentTimeMillis() - lastUse;
            if (age <= TotemData.MAX_TRACKED_INTERVAL_MS) {
                state = "wait:" + age + "ms";
                stateColor = Palette.WARN;
            } else {
                state = "stale";
                stateColor = Palette.CONNECTIVE;
            }
        } else {
            state = "idle";
            stateColor = Palette.CONNECTIVE;
        }

        String offhandState = offhandTotem ? "totem" : "empty";
        TextColor offhandColor = offhandTotem ? Palette.SUCCESS : Palette.DANGER;

        Long lastInterval = intervals.isEmpty() ? null : intervals.get(intervals.size() - 1);
        String lastDelay = lastInterval == null ? "-" : lastInterval + "ms";

        String average = intervals.isEmpty() ? "-" : formatDouble(MathUtil.getMean(intervals)) + "ms";
        String deviation = intervals.size() < 2 ? "-" : formatDouble(MathUtil.getStandardDeviation(intervals)) + "ms";

        Component line = Component.empty()
                .append(label("OH "))
                .append(value(offhandState, offhandColor))
                .append(separator())
                .append(label("State "))
                .append(value(state, stateColor))
                .append(separator())
                .append(label("Last "))
                .append(value(lastDelay, lastInterval == null ? Palette.CONNECTIVE : Palette.VALUE))
                .append(separator())
                .append(label("Avg "))
                .append(value(average, intervals.isEmpty() ? Palette.CONNECTIVE : Palette.VALUE))
                .append(separator())
                .append(label("SD "))
                .append(value(deviation, intervals.size() < 2 ? Palette.CONNECTIVE : Palette.VALUE))
                .append(separator())
                .append(label("N "))
                .append(value(String.valueOf(intervals.size()), intervals.isEmpty() ? Palette.CONNECTIVE : Palette.BRAND));

        return DebugOverlayFrame.of(line);
    }
}
