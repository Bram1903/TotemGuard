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
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayFrame;
import com.deathmotion.totemguard.common.player.debug.DebugOverlayProvider;
import com.deathmotion.totemguard.common.player.movement.MovementEstimator;
import com.deathmotion.totemguard.common.player.movement.MovementResult;
import com.deathmotion.totemguard.common.util.Palette;
import com.github.retrooper.packetevents.protocol.world.Location;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public final class MovementDebugProvider implements DebugOverlayProvider {

    private static Component field(String label, String value, TextColor valueColor) {
        return Component.text(label, Palette.LABEL).append(Component.text(value + " ", valueColor));
    }

    @Override
    public String getKey() {
        return "movement";
    }

    @Override
    public String getDisplayName() {
        return "Movement";
    }

    @Override
    public DebugOverlayFrame buildFrame(TGPlayer player) {
        Data data = player.getData();
        MovementEstimator estimator = data.getMovementEstimator();
        MovementData movement = data.getMovementData();

        Location current = movement.getCurrent();
        Location previous = movement.getPrevious();
        double speed = Math.hypot(current.getX() - previous.getX(), current.getZ() - previous.getZ());
        double vSpeed = current.getY() - previous.getY();

        boolean moved = estimator.getResult() == MovementResult.MOVED;
        boolean ascending = estimator.isAscendingThisTick();

        Component horizontal = Component.empty()
                .append(field("H ", moved ? "moved" : "ok", moved ? Palette.DANGER : Palette.SUCCESS))
                .append(field("sp", String.format("%.3f", speed), Palette.BRAND))
                .append(field("ex", String.format("%.4f", estimator.getLastExcess()), Palette.BRAND))
                .append(field("", estimator.windowHits() + "/" + estimator.hitsForMoved(), Palette.CAPTION));

        Component vertical = Component.empty()
                .append(field("V ", ascending ? "asc" : "ok", ascending ? Palette.DANGER : Palette.SUCCESS))
                .append(field("vy", String.format("%.3f", vSpeed), Palette.BRAND))
                .append(field("ex", String.format("%.4f", estimator.getLastVerticalExcess()), Palette.BRAND))
                .append(field("", estimator.getVerticalCause().name().toLowerCase(), Palette.CAPTION));

        return DebugOverlayFrame.of(horizontal, vertical);
    }
}
