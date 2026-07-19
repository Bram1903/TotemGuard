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

public record RiderControl(
        double moveSpeed,
        double gravity,
        double stepHeight,
        double jumpStrength,
        double jumpTakeoff,
        double lookX,
        double lookZ,
        double leapRadius,
        double dashVertical,
        boolean steerable,
        boolean canFloatInWater,
        boolean jumpTick) implements ControlEnvelope {

    public static final double WATER_FRICTION = 0.8;
    public static final double WATER_ACCEL = 0.02;

    private static final double AIR_ACCEL_FACTOR = 0.1;

    @Override
    public double airAccel() {
        return moveSpeed * AIR_ACCEL_FACTOR;
    }

    @Override
    public double fluidFriction() {
        return WATER_FRICTION;
    }

    @Override
    public double fluidAccel() {
        return WATER_ACCEL;
    }
}
