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
import com.deathmotion.totemguard.common.physics.medium.MediumModel;

public final class ClimbModel implements MediumModel {

    private static final double HORIZONTAL_MAX = 0.15 * Math.sqrt(2.0);
    private static final double ASCENT = 0.2;
    private static final double DESCENT = -0.15;
    private static final double AIR_FRICTION = 0.91;

    @Override
    public MediumKind kind() {
        return MediumKind.CLIMB;
    }

    @Override
    public double accelBound(ControlEnvelope input, GroundFacts ground) {
        return HORIZONTAL_MAX;
    }

    @Override
    public void horizontalOptions(ControlEnvelope input, GroundFacts ground, AreaBounds bounds) {
        bounds.centerX(0.0);
        bounds.centerZ(0.0);
        bounds.radius(HORIZONTAL_MAX);
    }

    // Falling past a ladder without climbing it must not flag, so the descent clamp is no floor.
    @Override
    public void verticalOptions(ControlEnvelope input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        double ascent = ASCENT;
        if (input.jumpPossible()) ascent = Math.max(ascent, input.jumpTakeoff());
        if (ground.groundedStart() || ground.groundedEnd()) ascent = Math.max(ascent, input.stepHeight());
        if (input.fluidExitHop()) ascent = Math.max(ascent, MotionDefaults.FLUID_EXIT_HOP);
        bounds.raiseCeiling(ascent);
        bounds.lowerFloor(DESCENT);
        bounds.enforceDescentFloor(false);
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        return AIR_FRICTION;
    }

    @Override
    public double advanceVertical(double verticalVelocity, ControlEnvelope input) {
        return verticalVelocity;
    }
}
