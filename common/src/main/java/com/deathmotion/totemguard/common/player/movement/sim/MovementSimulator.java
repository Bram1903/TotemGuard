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

package com.deathmotion.totemguard.common.player.movement.sim;

import com.deathmotion.totemguard.common.player.movement.area.MotionArea;
import com.deathmotion.totemguard.common.player.movement.area.Range;
import com.deathmotion.totemguard.common.player.movement.world.BlockEnvironment;

import static com.deathmotion.totemguard.common.player.movement.MovementConstants.*;

public final class MovementSimulator {

    private static final double AIR_FRICTION = MAX_HORIZONTAL_FRICTION;

    private static final double CLIMB_HORIZONTAL_MAX = 0.15 * Math.sqrt(2.0);
    private static final double CLIMB_ASCENT = 0.2;
    private static final double CLIMB_DESCENT = -0.15;

    private static final double FLUID_FRICTION = 0.96;
    private static final double FLUID_ACCEL = 0.15;
    private static final double FLUID_ASCENT = 0.75;
    private static final double FLUID_DESCENT = -10.0;

    private static final double STUCK_DESCENT = -10.0;

    private MovementSimulator() {
    }

    public static MotionArea predictMove(MotionArea carried, MovementInput in, BlockEnvironment env) {
        if (env.fluid()) return predictFluid(carried);
        if (env.stuck()) return predictStuck(carried, in, env);
        if (env.climbable()) return predictClimb();
        return predictLand(carried, in, env);
    }

    public static MotionArea advance(double legalSpeed, double legalVerticalVelocity, MovementInput in, BlockEnvironment env) {
        if (env.fluid()) return advanceFluid(legalSpeed, legalVerticalVelocity);
        if (env.stuck()) return advanceStuck();
        if (env.climbable()) return advanceClimb(legalSpeed, legalVerticalVelocity);
        return advanceLand(legalSpeed, legalVerticalVelocity, in, env);
    }

    private static MotionArea predictLand(MotionArea carried, MovementInput in, BlockEnvironment env) {
        double accel = in.horizontalInput() ? landAcceleration(in, env) : 0.0;
        Range horizontal = carried.horizontalSpeed().expand(accel).clampToNonNegative();
        if (in.sprintJump()) horizontal = horizontal.grow(0.0, SPRINT_JUMP_BOOST);

        Range vertical = carried.vertical();
        if (in.jumpPossible()) vertical = vertical.raiseCeiling(in.jumpStrength() + in.jumpBoostPower());
        if (in.lastOnGround()) vertical = vertical.raiseCeiling(in.stepHeight());

        return new MotionArea(horizontal, vertical);
    }

    private static MotionArea advanceLand(double legalSpeed, double legalVerticalVelocity, MovementInput in, BlockEnvironment env) {
        double friction = in.lastOnGround() ? env.slipperiness() * AIR_FRICTION : AIR_FRICTION;
        double nextHorizontal = legalSpeed * friction;

        double moveVertical = in.onGround() ? 0.0 : legalVerticalVelocity;
        return MotionArea.of(Math.max(0.0, nextHorizontal), endOfTickVertical(moveVertical, in));
    }

    private static double landAcceleration(MovementInput in, BlockEnvironment env) {
        if (in.lastOnGround()) {
            double slip = env.slipperiness();
            return in.movementSpeed() * GROUND_ACCEL_NUMERATOR / (slip * slip * slip);
        }
        return in.sprinting() ? AIR_ACCEL_SPRINTING : AIR_ACCEL;
    }

    private static MotionArea predictClimb() {
        return new MotionArea(new Range(0.0, CLIMB_HORIZONTAL_MAX), new Range(CLIMB_DESCENT, CLIMB_ASCENT));
    }

    private static MotionArea advanceClimb(double legalSpeed, double legalVerticalVelocity) {
        return MotionArea.of(legalSpeed * AIR_FRICTION, legalVerticalVelocity);
    }

    private static MotionArea predictStuck(MotionArea carried, MovementInput in, BlockEnvironment env) {
        double accel = in.horizontalInput() ? landAcceleration(in, env) : 0.0;
        double horizontalBase = carried.horizontalSpeed().max() + accel;
        if (in.sprintJump()) horizontalBase += SPRINT_JUMP_BOOST;
        double horizontalCap = horizontalBase * env.stuckHorizontal();

        double ceiling = carried.vertical().max();
        if (in.jumpPossible()) ceiling = Math.max(ceiling, in.jumpStrength() + in.jumpBoostPower());
        if (in.lastOnGround()) ceiling = Math.max(ceiling, in.stepHeight());
        double ascent = Math.max(0.0, ceiling) * Math.min(1.0, env.stuckVertical());

        return new MotionArea(new Range(0.0, horizontalCap), new Range(STUCK_DESCENT, ascent));
    }

    private static MotionArea advanceStuck() {
        return MotionArea.resting();
    }

    private static MotionArea predictFluid(MotionArea carried) {
        double maxHorizontal = carried.horizontalSpeed().max() * FLUID_FRICTION + FLUID_ACCEL;
        return new MotionArea(new Range(0.0, maxHorizontal), new Range(FLUID_DESCENT, FLUID_ASCENT));
    }

    private static MotionArea advanceFluid(double legalSpeed, double legalVerticalVelocity) {
        return MotionArea.of(legalSpeed * FLUID_FRICTION, legalVerticalVelocity * 0.8);
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
