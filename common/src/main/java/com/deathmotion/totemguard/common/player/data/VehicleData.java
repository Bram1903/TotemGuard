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

package com.deathmotion.totemguard.common.player.data;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = false)
public final class VehicleData {

    private static final int IMPULSE_WINDOW = 3;
    private static final int JUMP_CLAIM_GROUNDED_MOVES = 3;
    private boolean hasPosition;
    private double prevX, prevY, prevZ;
    private double curX, curY, curZ;
    private float yaw, pitch;
    private double impulseX, impulseY, impulseZ;
    private int impulseTicks;
    private double jumpClaimScale;
    private int jumpClaimBudget;
    private boolean driverSeat;

    private boolean seedFromMount = true;

    public void handleMove(double x, double y, double z, float yaw, float pitch) {
        if (!hasPosition || seedFromMount) {
            prevX = x;
            prevY = y;
            prevZ = z;
            hasPosition = true;
            seedFromMount = false;
        } else {
            prevX = curX;
            prevY = curY;
            prevZ = curZ;
        }
        curX = x;
        curY = y;
        curZ = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double deltaX() {
        return curX - prevX;
    }

    public double deltaY() {
        return curY - prevY;
    }

    public double deltaZ() {
        return curZ - prevZ;
    }

    public void onMount() {
        seedFromMount = true;
    }

    public void addImpulse(double x, double y, double z) {
        impulseX = x;
        impulseY = y;
        impulseZ = z;
        impulseTicks = IMPULSE_WINDOW;
    }

    public boolean impulseActive() {
        return impulseTicks > 0;
    }

    public void tickImpulse() {
        if (impulseTicks > 0) impulseTicks--;
    }

    public void consumeImpulse() {
        impulseTicks = 0;
    }

    public void onJumpClaim(int power) {
        int clamped = Math.max(0, Math.min(100, power));
        jumpClaimScale = clamped >= 90 ? 1.0 : 0.4F + 0.4F * clamped / 90.0F;
        jumpClaimBudget = JUMP_CLAIM_GROUNDED_MOVES;
    }

    public boolean hasJumpClaim() {
        return jumpClaimBudget > 0;
    }

    public double jumpClaimScale() {
        return jumpClaimScale;
    }

    public void tickJumpClaimGrounded() {
        if (jumpClaimBudget > 0) jumpClaimBudget--;
    }

    public void reset() {
        hasPosition = false;
        seedFromMount = true;
        impulseTicks = 0;
        jumpClaimBudget = 0;
        driverSeat = false;
    }
}
