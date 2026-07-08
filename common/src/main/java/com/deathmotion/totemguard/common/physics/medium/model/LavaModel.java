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
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.input.PlayerInput;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;

public final class LavaModel implements MediumModel {

    private static final double HORIZONTAL_DRAG = 0.5;
    private static final double VERTICAL_DRAG = 0.8;
    private static final double SWIM_IMPULSE = 0.04;
    private static final double ASCENT_MIN = 0.1;

    @Override
    public MediumKind kind() {
        return MediumKind.LAVA;
    }

    @Override
    public double accelBound(PlayerInput input, GroundFacts ground) {
        return input.fluidAccel();
    }

    @Override
    public void verticalOptions(PlayerInput input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        double lavaGravity = input.gravity() / 4.0;
        bounds.ceiling(Math.max(ASCENT_MIN, bounds.ceiling() + SWIM_IMPULSE));
        bounds.floor(bounds.floor() - lavaGravity - SWIM_IMPULSE);
        if (input.jumpPossible()) bounds.raiseCeiling(input.jumpTakeoff());
        if (ground.groundedStart() || ground.groundedEnd()
                || contact.nearestSupportGap() <= input.stepHeight()) {
            bounds.raiseCeiling(input.stepHeight());
        }
        bounds.enforceDescentFloor(true);
    }

    @Override
    public double frictionMax(PlayerInput input, GroundFacts ground) {
        return HORIZONTAL_DRAG;
    }

    @Override
    public double advanceVertical(double verticalVelocity, PlayerInput input) {
        return verticalVelocity * VERTICAL_DRAG;
    }
}
