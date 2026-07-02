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

import com.deathmotion.totemguard.common.physics.MovementConstants;

public class PlayerAttributeData {

    private double scale = 1.0;
    private double movementSpeed = MovementConstants.BASE_MOVEMENT_SPEED;
    private double jumpStrength = MovementConstants.JUMP_POWER;
    private double gravity = MovementConstants.GRAVITY;
    private double stepHeight = MovementConstants.STEP_HEIGHT;
    private double sneakingSpeed = MovementConstants.SNEAKING_SPEED;
    private double movementEfficiency = MovementConstants.MOVEMENT_EFFICIENCY;
    private double waterMovementEfficiency = MovementConstants.WATER_MOVEMENT_EFFICIENCY;
    private double flyingSpeed = MovementConstants.FLYING_SPEED;
    private double safeFallDistance = MovementConstants.SAFE_FALL_DISTANCE;

    public void setScale(double scale) {
        this.scale = Math.max(0.0625, Math.min(16.0, scale));
    }

    public void setMovementSpeed(double movementSpeed) {
        this.movementSpeed = Math.max(0.0, Math.min(1024.0, movementSpeed));
    }

    public void setJumpStrength(double jumpStrength) {
        this.jumpStrength = Math.max(0.0, Math.min(32.0, jumpStrength));
    }

    public void setGravity(double gravity) {
        this.gravity = Math.max(-1.0, Math.min(1.0, gravity));
    }

    public void setStepHeight(double stepHeight) {
        this.stepHeight = Math.max(0.0, Math.min(10.0, stepHeight));
    }

    public void setSneakingSpeed(double sneakingSpeed) {
        this.sneakingSpeed = Math.max(0.0, Math.min(1.0, sneakingSpeed));
    }

    public void setMovementEfficiency(double movementEfficiency) {
        this.movementEfficiency = Math.max(0.0, Math.min(1.0, movementEfficiency));
    }

    public void setWaterMovementEfficiency(double waterMovementEfficiency) {
        this.waterMovementEfficiency = Math.max(0.0, Math.min(1.0, waterMovementEfficiency));
    }

    public void setFlyingSpeed(double flyingSpeed) {
        this.flyingSpeed = Math.max(0.0, Math.min(1024.0, flyingSpeed));
    }

    public void setSafeFallDistance(double safeFallDistance) {
        this.safeFallDistance = Math.max(0.0, Math.min(1024.0, safeFallDistance));
    }

    public double width() {
        return MovementConstants.BASE_WIDTH * scale;
    }

    public double height() {
        return MovementConstants.STANDING_HEIGHT * scale;
    }

    public double scale() {
        return scale;
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public double jumpStrength() {
        return jumpStrength;
    }

    public double gravity() {
        return gravity;
    }

    public double stepHeight() {
        return stepHeight;
    }

    public double sneakingSpeed() {
        return sneakingSpeed;
    }

    public double movementEfficiency() {
        return movementEfficiency;
    }

    public double waterMovementEfficiency() {
        return waterMovementEfficiency;
    }

    public double flyingSpeed() {
        return flyingSpeed;
    }

    public double safeFallDistance() {
        return safeFallDistance;
    }

    public void reset() {
        scale = 1.0;
        movementSpeed = MovementConstants.BASE_MOVEMENT_SPEED;
        jumpStrength = MovementConstants.JUMP_POWER;
        gravity = MovementConstants.GRAVITY;
        stepHeight = MovementConstants.STEP_HEIGHT;
        sneakingSpeed = MovementConstants.SNEAKING_SPEED;
        movementEfficiency = MovementConstants.MOVEMENT_EFFICIENCY;
        waterMovementEfficiency = MovementConstants.WATER_MOVEMENT_EFFICIENCY;
        flyingSpeed = MovementConstants.FLYING_SPEED;
        safeFallDistance = MovementConstants.SAFE_FALL_DISTANCE;
    }
}
