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

public record BoatControl(
        double dirX,
        double dirZ,
        double reachMin,
        double reachMax) implements ControlEnvelope {

    @Override
    public boolean inventoryOpen() {
        return false;
    }

    @Override
    public boolean horizontalInput() {
        return false;
    }

    @Override
    public boolean sneaking() {
        return false;
    }

    @Override
    public boolean sprinting() {
        return false;
    }

    @Override
    public boolean sprintJump() {
        return false;
    }

    @Override
    public boolean jumpPossible() {
        return false;
    }

    @Override
    public boolean ceilingClampedJump() {
        return false;
    }

    @Override
    public boolean fluidExitHop() {
        return false;
    }

    @Override
    public boolean priorWallContact() {
        return false;
    }

    @Override
    public double moveSpeed() {
        return 0.0;
    }

    @Override
    public double airAccel() {
        return 0.0;
    }

    @Override
    public double jumpStrength() {
        return 0.0;
    }

    @Override
    public double gravity() {
        return 0.0;
    }

    @Override
    public double stepHeight() {
        return 0.0;
    }

    @Override
    public boolean levitation() {
        return false;
    }

    @Override
    public int levitationAmplifier() {
        return 0;
    }

    @Override
    public boolean slowFalling() {
        return false;
    }

    @Override
    public double fluidFriction() {
        return 0.0;
    }

    @Override
    public double fluidAccel() {
        return 0.0;
    }

    @Override
    public double sprintJumpResidual() {
        return 0.0;
    }

    @Override
    public double lookX() {
        return 0.0;
    }

    @Override
    public double lookY() {
        return 0.0;
    }

    @Override
    public double lookZ() {
        return 0.0;
    }

    @Override
    public double pitchDegrees() {
        return 0.0;
    }

    @Override
    public boolean swimming() {
        return false;
    }

    @Override
    public double prevLookX() {
        return 0.0;
    }

    @Override
    public double prevLookY() {
        return 0.0;
    }

    @Override
    public double prevLookZ() {
        return 0.0;
    }

    @Override
    public double jumpBoostPower() {
        return 0.0;
    }

    @Override
    public double jumpTakeoff() {
        return 0.0;
    }

    @Override
    public double lookHorizontal() {
        return 0.0;
    }
}
