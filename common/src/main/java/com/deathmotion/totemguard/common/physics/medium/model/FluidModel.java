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

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.input.PlayerInput;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;

public abstract class FluidModel implements MediumModel {

    protected static final double VERTICAL_DRAG = 0.8;
    protected static final double SWIM_IMPULSE = 0.04;
    protected static final double ASCENT_MIN = 0.1;
    protected static final double WALL_BUMP_ASCENT = 0.34;

    protected abstract double gravityDivisor();

    @Override
    public double accelBound(PlayerInput input, GroundFacts ground) {
        return input.fluidAccel();
    }

    @Override
    public final void verticalOptions(PlayerInput input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        double gravity = input.gravity() / gravityDivisor();
        bounds.ceiling(Math.max(ASCENT_MIN, bounds.ceiling() + SWIM_IMPULSE));
        bounds.floor(bounds.floor() - gravity - SWIM_IMPULSE);
        if (input.jumpPossible()) bounds.raiseCeiling(input.jumpTakeoff());
        if (ground.groundedStart() || ground.groundedEnd()
                || contact.nearestSupportGap() <= input.stepHeight()) {
            bounds.raiseCeiling(input.stepHeight());
        }
        ascentOptions(input, contact, bounds);
        bounds.enforceDescentFloor(true);
    }

    protected void ascentOptions(PlayerInput input, ContactReport contact, AreaBounds bounds) {
        if (contact.wallNear()) bounds.raiseCeiling(WALL_BUMP_ASCENT);
    }

    @Override
    public double advanceVertical(double verticalVelocity, PlayerInput input) {
        return verticalVelocity * VERTICAL_DRAG;
    }
}
