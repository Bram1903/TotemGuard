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

package com.deathmotion.totemguard.common.physics.medium.model;

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.control.RiderControl;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;

public final class RiderWaterModel extends FluidModel {

    public static final double FLOAT_RISE = 0.04;

    public static double floatRise(ControlEnvelope input) {
        return input instanceof RiderControl rider && rider.canFloatInWater() ? FLOAT_RISE : 0.0;
    }

    @Override
    public MediumKind kind() {
        return MediumKind.WATER;
    }

    @Override
    protected double gravityDivisor() {
        return MotionDefaults.WATER_GRAVITY_DIVISOR;
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        return input.fluidFriction();
    }

    @Override
    public double advanceVertical(double verticalVelocity, ControlEnvelope input) {
        return verticalVelocity * MotionDefaults.FLUID_VERTICAL_DRAG
                - input.gravity() / MotionDefaults.WATER_GRAVITY_DIVISOR;
    }
}
