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

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.input.PlayerInput;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;

public final class LandModel implements MediumModel {

    public static final double AIR_FRICTION = 0.91;
    private static final double GROUND_ACCEL_NUMERATOR = 0.21600002;
    // Always the sprint value: a sprint-flag desync must never turn into a false positive.
    public static final double AIR_ACCEL = 0.026;
    private static final double LEVITATION_PER_LEVEL = 0.05;
    private static final double LEVITATION_RATE = 0.2;
    private static final double VERTICAL_DRAG = 0.98;
    private static final double SLOW_FALLING_GRAVITY = 0.01;

    @Override
    public MediumKind kind() {
        return MediumKind.LAND;
    }

    @Override
    public double accelBound(PlayerInput input, GroundFacts ground) {
        double groundBound = groundAccel(input, ground);
        return switch (ground.start()) {
            case SUPPORTED -> groundBound;
            case AMBIGUOUS -> Math.max(groundBound, AIR_ACCEL);
            case AIRBORNE -> AIR_ACCEL;
        };
    }

    @Override
    public void verticalOptions(PlayerInput input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        if (input.jumpPossible()) bounds.raiseCeiling(input.jumpTakeoff());
        if (input.fluidExitHop()) bounds.raiseCeiling(MotionDefaults.FLUID_EXIT_HOP);
        if (ground.groundedStart() || ground.groundedEnd() || contact.startOverlapping()) {
            bounds.raiseCeiling(input.stepHeight());
        }
        bounds.lowerFloor(advanceVertical(0.0, input));
        bounds.enforceDescentFloor(true);
    }

    @Override
    public double frictionMax(PlayerInput input, GroundFacts ground) {
        boolean groundDrag = ground.supportedStart() && !input.ceilingClampedJump();
        return groundDrag ? ground.startSlipMax() * AIR_FRICTION : AIR_FRICTION;
    }

    @Override
    public double advanceVertical(double verticalVelocity, PlayerInput input) {
        if (input.levitation()) {
            double target = LEVITATION_PER_LEVEL * (input.levitationAmplifier() + 1);
            return (verticalVelocity + (target - verticalVelocity) * LEVITATION_RATE) * VERTICAL_DRAG;
        }
        double gravity = (input.slowFalling() && verticalVelocity <= 0.0)
                ? Math.min(input.gravity(), SLOW_FALLING_GRAVITY)
                : input.gravity();
        return (verticalVelocity - gravity) * VERTICAL_DRAG;
    }

    private static double groundAccel(PlayerInput input, GroundFacts ground) {
        double slip = ground.startSlipMin();
        return input.moveSpeed() * GROUND_ACCEL_NUMERATOR / (slip * slip * slip);
    }
}
