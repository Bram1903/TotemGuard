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

package com.deathmotion.totemguard.common.physics.sim;

import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.area.Range;
import com.deathmotion.totemguard.common.world.scan.BlockEnvironment;

import static com.deathmotion.totemguard.common.physics.MovementConstants.*;

public final class MovementSimulator {

    private static final double AIR_FRICTION = MAX_HORIZONTAL_FRICTION;

    private static final double CLIMB_HORIZONTAL_MAX = 0.15 * Math.sqrt(2.0);
    private static final double CLIMB_ASCENT = 0.2;
    private static final double CLIMB_DESCENT = -0.15;

    private static final double FLUID_VERTICAL_DRAG = 0.8;
    private static final double SWIM_VERTICAL_IMPULSE = 0.04;
    private static final double FLUID_ASCENT_MIN = 0.1;

    private static final double STUCK_DESCENT = -10.0;

    private MovementSimulator() {
    }

    public static MotionArea predictMove(MotionArea carried, MovementInput in, BlockEnvironment env) {
        if (env.fluid()) return predictFluid(carried, in);
        if (env.stuck()) return predictStuck(carried, in, env);
        if (env.climbable()) return predictClimb(in);
        return predictLand(carried, in);
    }

    public static MotionArea advance(double legalSpeed, double legalVerticalVelocity, MovementInput in, BlockEnvironment env) {
        if (env.fluid()) return advanceFluid(legalSpeed, legalVerticalVelocity, in);
        if (env.stuck()) return advanceStuck();
        if (env.climbable()) return advanceClimb(legalSpeed, legalVerticalVelocity);
        return advanceLand(legalSpeed, legalVerticalVelocity, in);
    }

    public static MotionArea advanceBounce(MotionArea advanced, double incomingVertical, double bounceFactor, MovementInput in) {
        if (incomingVertical >= 0.0 || bounceFactor <= 0.0) return advanced;
        double reflected = -incomingVertical * bounceFactor;
        return MotionArea.of(advanced.horizontalSpeed().max(), endOfTickVertical(reflected, in));
    }

    public static double restFallVelocity(MovementInput in) {
        return endOfTickVertical(0.0, in);
    }

    private static MotionArea predictLand(MotionArea carried, MovementInput in) {
        double accel = in.horizontalInput() ? landAcceleration(in) : 0.0;
        Range horizontal = carried.horizontalSpeed().expand(accel).clampToNonNegative();
        if (in.sprintJump()) horizontal = horizontal.grow(0.0, SPRINT_JUMP_BOOST);

        Range vertical = carried.vertical();
        if (in.jumpPossible()) vertical = vertical.raiseCeiling(in.jumpStrength() + in.jumpBoostPower());
        if (in.waterExitHop()) vertical = vertical.raiseCeiling(WATER_EXIT_HOP);
        if (((in.groundedStart() || in.groundedEnd()) && in.supportWithinStep()) || in.startOverlapping()) {
            vertical = vertical.raiseCeiling(in.stepHeight());
        }
        if (in.groundedEnd()) vertical = vertical.raiseCeiling(0.0);
        if (in.startOverlapping()) vertical = vertical.raiseCeiling(0.0);
        if (in.bubbleAscent() > 0.0) vertical = vertical.raiseCeiling(in.bubbleAscent());

        return new MotionArea(horizontal, vertical);
    }

    private static MotionArea advanceLand(double legalSpeed, double legalVerticalVelocity, MovementInput in) {
        boolean groundDrag = in.groundedStart() && !in.ceilingClampedJump();
        double friction = groundDrag ? in.slipperinessMax() * AIR_FRICTION : AIR_FRICTION;
        double nextHorizontal = legalSpeed * in.blockSpeedFactor() * friction;

        double moveVertical = in.groundedEnd() ? 0.0 : legalVerticalVelocity;
        return MotionArea.of(Math.max(0.0, nextHorizontal), endOfTickVertical(moveVertical, in));
    }

    private static double landAcceleration(MovementInput in) {
        double slip = in.slipperinessMin();
        double ground = in.movementSpeed() * GROUND_ACCEL_NUMERATOR / (slip * slip * slip);
        if (in.groundedStart()) return ground;
        double air = in.sprinting() ? AIR_ACCEL_SPRINTING : AIR_ACCEL;
        return in.groundedStartAmbiguous() ? Math.max(ground, air) : air;
    }

    private static MotionArea predictClimb(MovementInput in) {
        double ascent = CLIMB_ASCENT;
        if (in.jumpPossible()) ascent = Math.max(ascent, in.jumpStrength() + in.jumpBoostPower());
        if (in.groundedStart() || in.groundedEnd()) ascent = Math.max(ascent, in.stepHeight());
        return new MotionArea(new Range(0.0, CLIMB_HORIZONTAL_MAX), new Range(CLIMB_DESCENT, ascent));
    }

    private static MotionArea advanceClimb(double legalSpeed, double legalVerticalVelocity) {
        return MotionArea.of(legalSpeed * AIR_FRICTION, legalVerticalVelocity);
    }

    private static MotionArea predictStuck(MotionArea carried, MovementInput in, BlockEnvironment env) {
        double accel = in.horizontalInput() ? landAcceleration(in) : 0.0;
        double horizontalBase = carried.horizontalSpeed().max() + accel;
        if (in.sprintJump()) horizontalBase += SPRINT_JUMP_BOOST;
        double horizontalCap = horizontalBase * env.stuckHorizontal();

        double ceiling = carried.vertical().max();
        if (in.jumpPossible()) ceiling = Math.max(ceiling, in.jumpStrength() + in.jumpBoostPower());
        if (in.groundedStart()) ceiling = Math.max(ceiling, in.stepHeight());
        double ascent = Math.max(0.0, ceiling) * Math.min(1.0, env.stuckVertical());

        return new MotionArea(new Range(0.0, horizontalCap), new Range(STUCK_DESCENT, ascent));
    }

    private static MotionArea advanceStuck() {
        return MotionArea.resting();
    }

    private static MotionArea predictFluid(MotionArea carried, MovementInput in) {
        double maxHorizontal = carried.horizontalSpeed().max() + in.fluidAccel();
        double waterGravity = in.gravity() / 16.0;
        double ceiling = Math.max(FLUID_ASCENT_MIN, carried.vertical().max() + SWIM_VERTICAL_IMPULSE);
        double floor = carried.vertical().min() - waterGravity - SWIM_VERTICAL_IMPULSE;
        Range vertical = new Range(floor, ceiling);
        if (in.groundedStart() || in.groundedEnd() || in.supportWithinStep()) {
            vertical = vertical.raiseCeiling(in.stepHeight());
        }
        if (in.bubbleAscent() > 0.0) vertical = vertical.raiseCeiling(in.bubbleAscent());
        return new MotionArea(new Range(0.0, maxHorizontal), vertical);
    }

    private static MotionArea advanceFluid(double legalSpeed, double legalVerticalVelocity, MovementInput in) {
        return MotionArea.of(legalSpeed * in.fluidFriction(), legalVerticalVelocity * FLUID_VERTICAL_DRAG);
    }

    private static double endOfTickVertical(double velocityY, MovementInput in) {
        if (in.levitation()) {
            double target = LEVITATION_PER_LEVEL * (in.levitationAmplifier() + 1);
            return (velocityY + (target - velocityY) * LEVITATION_RATE) * VERTICAL_DRAG;
        }
        double gravity = (in.slowFalling() && velocityY <= 0.0) ? Math.min(in.gravity(), SLOW_FALLING_GRAVITY) : in.gravity();
        return (velocityY - gravity) * VERTICAL_DRAG;
    }
}
