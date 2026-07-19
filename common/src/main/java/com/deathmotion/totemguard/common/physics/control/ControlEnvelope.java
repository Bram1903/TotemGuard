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

import com.deathmotion.totemguard.common.util.ClientMath;

public interface ControlEnvelope {

    default boolean inventoryOpen() {
        return false;
    }

    default boolean horizontalInput() {
        return true;
    }

    default boolean sneaking() {
        return false;
    }

    default boolean sprinting() {
        return false;
    }

    default boolean sprintJump() {
        return false;
    }

    default boolean jumpPossible() {
        return false;
    }

    default boolean ceilingClampedJump() {
        return false;
    }

    default boolean fluidExitHop() {
        return false;
    }

    default boolean powderSnowClimb() {
        return false;
    }

    default boolean priorWallContact() {
        return false;
    }

    default double moveSpeed() {
        return 0.0;
    }

    default double airAccel() {
        return 0.0;
    }

    default double jumpStrength() {
        return 0.0;
    }

    default double gravity() {
        return 0.0;
    }

    default double stepHeight() {
        return 0.0;
    }

    default boolean levitation() {
        return false;
    }

    default int levitationAmplifier() {
        return 0;
    }

    default boolean slowFalling() {
        return false;
    }

    default double fluidFriction() {
        return 0.0;
    }

    default double fluidAccel() {
        return 0.0;
    }

    default double lookX() {
        return 0.0;
    }

    default double lookY() {
        return 0.0;
    }

    default double lookZ() {
        return 0.0;
    }

    default double pitchDegrees() {
        return 0.0;
    }

    default boolean swimming() {
        return false;
    }

    default double prevLookX() {
        return 0.0;
    }

    default double prevLookY() {
        return 0.0;
    }

    default double prevLookZ() {
        return 0.0;
    }

    default double jumpBoostPower() {
        return 0.0;
    }

    default double jumpTakeoff() {
        return 0.0;
    }

    default double lookHorizontal() {
        return ClientMath.horizontalDistance(lookX(), lookZ());
    }

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

    default double prevLookYAlt() {
        return prevLookY();
    }

    default double prevLookZAlt() {
        return prevLookZ();
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

    default double boostX() {
        return 0.0;
    }

    default double boostZ() {
        return 0.0;
    }

    default double boostSpread() {
        return 0.0;
    }

    default double frictionModifier() {
        return 1.0;
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
