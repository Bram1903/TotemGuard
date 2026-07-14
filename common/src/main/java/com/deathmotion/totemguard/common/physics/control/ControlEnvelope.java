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

package com.deathmotion.totemguard.common.physics.control;

public interface ControlEnvelope {

    boolean inventoryOpen();

    boolean horizontalInput();

    boolean sneaking();

    boolean sprinting();

    boolean sprintJump();

    boolean jumpPossible();

    boolean ceilingClampedJump();

    boolean fluidExitHop();

    default boolean powderSnowClimb() {
        return false;
    }

    boolean priorWallContact();

    double moveSpeed();

    double airAccel();

    double jumpStrength();

    double gravity();

    double stepHeight();

    boolean levitation();

    int levitationAmplifier();

    boolean slowFalling();

    double fluidFriction();

    double fluidAccel();

    double lookX();

    double lookY();

    double lookZ();

    double pitchDegrees();

    boolean swimming();

    double prevLookX();

    double prevLookY();

    double prevLookZ();

    double jumpBoostPower();

    double jumpTakeoff();

    double lookHorizontal();

    default double lookXAlt() {
        return lookX();
    }

    default double lookYAlt() {
        return lookY();
    }

    default double lookZAlt() {
        return lookZ();
    }

    default double prevLookXAlt() {
        return prevLookX();
    }

    default double flyAccel() {
        return 0.0;
    }

    default double flyVertical() {
        return 0.0;
    }

    default double airDragModifier() {
        return 1.0;
    }

    default double inputMultiplier() {
        return 1.0;
    }

    default double boostDirX() {
        return 0.0;
    }

    default double boostDirZ() {
        return 0.0;
    }

    default double boostSpread() {
        return 0.0;
    }

    default double frictionModifier() {
        return 1.0;
    }

    default double prevLookYAlt() {
        return prevLookY();
    }

    default double prevLookZAlt() {
        return prevLookZ();
    }

    default boolean claimedInputExact() {
        return false;
    }

    default double claimedWorldX() {
        return 0.0;
    }

    default double claimedWorldZ() {
        return 0.0;
    }

    default double claimedSpread() {
        return 0.0;
    }

    default double moveSpeedBase() {
        return moveSpeed();
    }

    default double airAccelBase() {
        return airAccel();
    }

    default double airAccelBaseMin() {
        return airAccelBase();
    }

    default double fluidAccelBase() {
        return fluidAccel();
    }

    default double flyAccelBase() {
        return flyAccel();
    }
}
