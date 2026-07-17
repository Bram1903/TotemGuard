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

package com.deathmotion.totemguard.common.physics.control;

import com.deathmotion.totemguard.common.world.entity.TrackedEntity;

public final class GhastControlResolver {

    private static final double FLYING_SPEED_DEFAULT = 0.05;
    private static final double ACCEL_SCALE = 3.9 * (5.0 / 3.0);
    private static final double INPUT_UP = 1.5;
    private static final double INPUT_DOWN = 1.0;
    private static final double INPUT_HORIZONTAL = Math.sqrt(0.98 * 0.98 + 1.0);

    private GhastControlResolver() {
    }

    public static GhastControl build(TrackedEntity ridden, boolean controlling) {
        if (!controlling) return new GhastControl(0.0, 0.0, 0.0);
        double flyingSpeed = Double.isNaN(ridden.flyingSpeed())
                ? FLYING_SPEED_DEFAULT
                : ridden.flyingSpeed();
        double accelScale = ACCEL_SCALE * flyingSpeed * flyingSpeed;
        return new GhastControl(accelScale * INPUT_HORIZONTAL, accelScale * INPUT_UP, accelScale * INPUT_DOWN);
    }
}
