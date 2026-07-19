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
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;

public final class WaterModel extends FluidModel {

    private static final int ENTRY_TICKS = 4;
    private static final double ENTRY_ASCENT = 0.75;
    private static final double STEER_RATE_STEEP = 0.085;
    private static final double STEER_RATE = 0.06;
    private static final double STEER_STEEP_LOOK = -0.2;
    private static final double CLIMB_RISE = 0.2 * MotionDefaults.FLUID_VERTICAL_DRAG;

    private int entryTicks;
    private boolean steerWater;
    private boolean climbable;

    private static double steered(double bound, double lookY) {
        double rate = lookY < STEER_STEEP_LOOK ? STEER_RATE_STEEP : STEER_RATE;
        return bound + (lookY - bound) * rate;
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
    protected void ascentOptions(ControlEnvelope input, ContactReport contact, AreaBounds bounds) {
        if (entryTicks > 0) {
            bounds.raiseCeiling(ENTRY_ASCENT);
        } else if (wallEvidence(input, contact)) {
            bounds.raiseCeiling(WALL_BUMP_ASCENT);
        }
        if (climbable) {
            bounds.raiseCeiling(CLIMB_RISE);
        }
        applySwimSteer(input, bounds);
    }

    private void applySwimSteer(ControlEnvelope input, AreaBounds bounds) {
        if (!input.swimming()) return;
        double steeredCeiling = Math.max(steered(bounds.ceiling(), input.lookY()),
                steered(bounds.ceiling(), input.lookYAlt()));
        double steeredFloor = Math.min(steered(bounds.floor(), input.lookY()),
                steered(bounds.floor(), input.lookYAlt()));
        if (input.lookY() <= 0.0 || steerWater) {
            bounds.ceiling(steeredCeiling);
            bounds.floor(steeredFloor);
        } else {
            bounds.ceiling(Math.max(bounds.ceiling(), steeredCeiling));
            bounds.floor(Math.min(bounds.floor(), steeredFloor));
        }
    }

    public void observe(MediumSample sample) {
        steerWater = sample.swimSteerWater();
        climbable = sample.climbable();
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        return input.fluidFriction();
    }

    public void advanceEntryWindow(boolean inFluidNow, boolean wasFluid) {
        if (!inFluidNow) {
            entryTicks = 0;
            return;
        }
        entryTicks = wasFluid ? Math.max(0, entryTicks - 1) : ENTRY_TICKS;
    }

    public void reset() {
        entryTicks = 0;
        climbable = false;
    }
}
