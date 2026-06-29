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

public final class MovementDebugProvider implements DebugOverlayProvider {

    private static Component label(String text) {
        return Component.text(text, Palette.LABEL);
    }

    private static Component separator() {
        return Component.text(" | ", Palette.SEPARATOR);
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
        boolean moved = estimator.getResult() == MovementResult.MOVED;
        boolean kb = data.getExternalVelocityData().hasHorizontal();

        Location current = movement.getCurrent();
        Location previous = movement.getPrevious();
        double speed = Math.hypot(current.getX() - previous.getX(), current.getZ() - previous.getZ());

        Component line = Component.empty()
                .append(label("Move "))
                .append(Component.text(moved ? "MOVING" : "not moving", moved ? Palette.DANGER : Palette.SUCCESS))
                .append(separator())
                .append(label("speed "))
                .append(Component.text(String.format("%.3f", speed), Palette.BRAND))
                .append(separator())
                .append(label("excess "))
                .append(Component.text(String.format("%.4f", estimator.getLastExcess()), Palette.BRAND))
                .append(separator())
                .append(label("hits "))
                .append(Component.text(estimator.windowHits() + "/" + estimator.hitsForMoved(), Palette.BRAND))
                .append(separator())
                .append(label("ground "))
                .append(Component.text(movement.isOnGround() ? "yes" : "no", Palette.CAPTION))
                .append(separator())
                .append(label("scale "))
                .append(Component.text(String.format("%.2f", data.getAttributeData().scale()), Palette.CAPTION))
                .append(separator())
                .append(label("kb "))
                .append(Component.text(kb ? "yes" : "no", kb ? Palette.WARN : Palette.SUCCESS));

        return DebugOverlayFrame.of(line);
    }
}
