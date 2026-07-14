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

public final class LandModel implements MediumModel {

    public static final double AIR_FRICTION = 0.91;
    public static final double POWDER_SNOW_CLIMB = 0.2;
    private static final double GROUND_ACCEL_NUMERATOR = 0.21600002;
    private static final double LEVITATION_PER_LEVEL = 0.05;
    private static final double LEVITATION_RATE = 0.2;
    private static final double VERTICAL_DRAG = 0.98;
    private static final double SLOW_FALLING_GRAVITY = 0.01;

    static double groundAccel(ControlEnvelope input, GroundFacts ground) {
        double slip = computeModifiedFriction(ground.startSlipMin(), input.frictionModifier());
        if (slip <= 0.6) return input.moveSpeed();
        return input.moveSpeed() * GROUND_ACCEL_NUMERATOR / (slip * slip * slip);
    }

    static double groundAccelBase(ControlEnvelope input, GroundFacts ground) {
        double slip = computeModifiedFriction(ground.startSlipMin(), input.frictionModifier());
        if (slip <= 0.6) return input.moveSpeedBase();
        return input.moveSpeedBase() * GROUND_ACCEL_NUMERATOR / (slip * slip * slip);
    }

    public static double verticalDrag(ControlEnvelope input) {
        return computeModifiedFriction(VERTICAL_DRAG, input.airDragModifier());
    }

    public static double computeModifiedFriction(double friction, double modifier) {
        if (modifier == 1.0) return friction;
        return Math.max(0.0, Math.min(1.0, 1.0 - (1.0 - friction) * modifier));
    }

    @Override
    public MediumKind kind() {
        return MediumKind.LAND;
    }

    @Override
    public double accelBound(ControlEnvelope input, GroundFacts ground) {
        double groundBound = groundAccel(input, ground);
        return switch (ground.start()) {
            case SUPPORTED -> groundBound;
            case AMBIGUOUS -> Math.max(groundBound, input.airAccel());
            case AIRBORNE -> input.airAccel();
        };
    }

    @Override
    public double accelBoundBase(ControlEnvelope input, GroundFacts ground) {
        double groundBound = groundAccelBase(input, ground);
        return switch (ground.start()) {
            case SUPPORTED -> groundBound;
            case AMBIGUOUS -> Math.max(groundBound, input.airAccelBase());
            case AIRBORNE -> input.airAccelBase();
        };
    }

    @Override
    public double accelBoundBaseMin(ControlEnvelope input, GroundFacts ground) {
        double groundBound = groundAccelBase(input, ground);
        return switch (ground.start()) {
            case SUPPORTED -> groundBound;
            case AMBIGUOUS -> Math.min(groundBound, input.airAccelBaseMin());
            case AIRBORNE -> input.airAccelBaseMin();
        };
    }

    @Override
    public void verticalOptions(ControlEnvelope input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        if (input.jumpPossible()) bounds.raiseCeiling(input.jumpTakeoff());
        if (input.fluidExitHop()) bounds.raiseCeiling(MotionDefaults.FLUID_EXIT_HOP);
        if (input.powderSnowClimb()) bounds.raiseCeiling(advanceVertical(POWDER_SNOW_CLIMB, input));
        if (ground.groundedStart() || ground.groundedEnd() || contact.startOverlapping()) {
            bounds.raiseCeiling(input.stepHeight());
        }
        bounds.lowerFloor(advanceVertical(0.0, input));
        bounds.enforceDescentFloor(true);
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        boolean groundDrag = ground.supportedStart() && !input.ceilingClampedJump();
        double airDrag = computeModifiedFriction(AIR_FRICTION, input.airDragModifier());
        if (!groundDrag) return airDrag;
        return computeModifiedFriction(ground.startSlipMax(), input.frictionModifier()) * airDrag;
    }

    @Override
    public double advanceVertical(double verticalVelocity, ControlEnvelope input) {
        double verticalDrag = verticalDrag(input);
        if (input.levitation()) {
            double target = LEVITATION_PER_LEVEL * (input.levitationAmplifier() + 1);
            return (verticalVelocity + (target - verticalVelocity) * LEVITATION_RATE) * verticalDrag;
        }
        double gravity = (input.slowFalling() && verticalVelocity <= 0.0)
                ? Math.min(input.gravity(), SLOW_FALLING_GRAVITY)
                : input.gravity();
        return (verticalVelocity - gravity) * verticalDrag;
    }
}
