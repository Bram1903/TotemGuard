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

package com.deathmotion.totemguard.common.physics.prescan;

import com.deathmotion.totemguard.common.physics.verdict.DeclineReason;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.world.border.BorderMirror;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import org.jetbrains.annotations.Nullable;

public final class DeclineCheck {

    private static final double BORDER_MARGIN = 2.0;

    private DeclineCheck() {
    }

    public static @Nullable DeclineReason check(Data data, BorderMirror border, double x, double z) {
        if (data.getGameMode() == GameMode.SPECTATOR) return DeclineReason.FLY;
        if (data.isInVehicle()) return DeclineReason.VEHICLE;
        if (data.isSleeping()) return DeclineReason.SLEEPING;
        if (border.isActive() && border.distanceToEdge(x, z) < BORDER_MARGIN) return DeclineReason.BORDER;
        return null;
    }
}
