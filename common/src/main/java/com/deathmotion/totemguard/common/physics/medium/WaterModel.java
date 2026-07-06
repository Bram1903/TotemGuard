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

package com.deathmotion.totemguard.common.physics.medium;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.input.PlayerInput;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;

public final class WaterModel implements MediumModel {

    private static final double VERTICAL_DRAG = 0.8;
    private static final double SWIM_IMPULSE = 0.04;
    private static final double ASCENT_MIN = 0.1;
    private static final int ENTRY_TICKS = 4;
    private static final double ENTRY_ASCENT = 0.75;
    private static final double WALL_BUMP_ASCENT = 0.34;

    private int entryTicks;

    @Override
    public MediumKind kind() {
        return MediumKind.WATER;
    }

    @Override
    public double accelBound(PlayerInput input, GroundFacts ground) {
        return input.fluidAccel();
    }

    @Override
    public void verticalOptions(PlayerInput input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        double waterGravity = input.gravity() / 16.0;
        bounds.ceiling(Math.max(ASCENT_MIN, bounds.ceiling() + SWIM_IMPULSE));
        bounds.floor(bounds.floor() - waterGravity - SWIM_IMPULSE);
        if (input.jumpPossible()) bounds.raiseCeiling(input.jumpTakeoff());
        if (ground.groundedStart() || ground.groundedEnd()
                || contact.nearestSupportGap() <= input.stepHeight()) {
            bounds.raiseCeiling(input.stepHeight());
        }
        if (entryTicks > 0) {
            bounds.raiseCeiling(ENTRY_ASCENT);
        } else if (contact.wallNear()) {
            bounds.raiseCeiling(WALL_BUMP_ASCENT);
        }
        bounds.enforceDescentFloor(true);
    }

    @Override
    public double frictionMax(PlayerInput input, GroundFacts ground) {
        return input.fluidFriction();
    }

    @Override
    public double advanceVertical(double verticalVelocity, PlayerInput input) {
        return verticalVelocity * VERTICAL_DRAG;
    }

    public void advanceEntryWindow(boolean inFluidNow, boolean wasFluid) {
        if (!inFluidNow) {
            entryTicks = 0;
            return;
        }
        entryTicks = wasFluid ? Math.max(0, entryTicks - 1) : ENTRY_TICKS;
    }

    public boolean entering() {
        return entryTicks > 0;
    }

    public void reset() {
        entryTicks = 0;
    }
}
