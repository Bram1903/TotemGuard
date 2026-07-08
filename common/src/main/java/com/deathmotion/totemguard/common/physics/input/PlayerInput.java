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

package com.deathmotion.totemguard.common.physics.input;

import com.deathmotion.totemguard.common.util.ClientMath;

public record PlayerInput(
        boolean inventoryOpen,
        boolean horizontalInput,
        boolean sneaking,
        boolean sprinting,
        boolean sprintJump,
        boolean jumpPossible,
        boolean ceilingClampedJump,
        boolean fluidExitHop,
        double moveSpeed,
        double jumpStrength,
        double gravity,
        double stepHeight,
        int jumpBoostAmplifier,
        boolean levitation,
        int levitationAmplifier,
        boolean slowFalling,
        double fluidFriction,
        double fluidAccel,
        double sprintJumpResidual,
        double lookX,
        double lookY,
        double lookZ,
        double pitchDegrees,
        boolean swimming,
        double prevLookX,
        double prevLookY,
        double prevLookZ) {

    public double jumpBoostPower() {
        return jumpBoostAmplifier >= 0 ? 0.1 * (jumpBoostAmplifier + 1) : 0.0;
    }

    public double jumpTakeoff() {
        return jumpStrength + jumpBoostPower();
    }

    public double lookHorizontal() {
        return ClientMath.horizontalDistance(lookX, lookZ);
    }
}
