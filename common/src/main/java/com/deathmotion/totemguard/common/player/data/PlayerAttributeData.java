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

import com.deathmotion.totemguard.common.physics.MotionDefaults;

public class PlayerAttributeData {

    private double scale = 1.0;
    private double movementSpeed = MotionDefaults.BASE_MOVEMENT_SPEED;
    private double jumpStrength = MotionDefaults.JUMP_POWER;
    private double gravity = MotionDefaults.GRAVITY;
    private double stepHeight = MotionDefaults.STEP_HEIGHT;
    private double sneakingSpeed = MotionDefaults.SNEAKING_SPEED;
    private double movementEfficiency = MotionDefaults.MOVEMENT_EFFICIENCY;
    private double waterMovementEfficiency = MotionDefaults.WATER_MOVEMENT_EFFICIENCY;
    private double flyingSpeed = MotionDefaults.FLYING_SPEED;
    private double safeFallDistance = MotionDefaults.SAFE_FALL_DISTANCE;
    private double fallDamageMultiplier = 1.0;
    private double airDragModifier = 1.0;
    private double frictionModifier = 1.0;
    private double blockBreakSpeed = 1.0;
    private double miningEfficiency = 0.0;
    private double submergedMiningSpeed = 0.2;
    private double blockInteractionRange = 4.5;

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

    public void setFallDamageMultiplier(double fallDamageMultiplier) {
        this.fallDamageMultiplier = Math.max(0.0, Math.min(100.0, fallDamageMultiplier));
    }

    public void setAirDragModifier(double airDragModifier) {
        this.airDragModifier = Math.max(0.0, Math.min(2048.0, airDragModifier));
    }

    public void setFrictionModifier(double frictionModifier) {
        this.frictionModifier = Math.max(0.0, Math.min(2048.0, frictionModifier));
    }

    public void setBlockBreakSpeed(double blockBreakSpeed) {
        this.blockBreakSpeed = Math.max(0.0, Math.min(1024.0, blockBreakSpeed));
    }

    public void setMiningEfficiency(double miningEfficiency) {
        this.miningEfficiency = Math.max(0.0, Math.min(1024.0, miningEfficiency));
    }

    public void setSubmergedMiningSpeed(double submergedMiningSpeed) {
        this.submergedMiningSpeed = Math.max(0.0, Math.min(20.0, submergedMiningSpeed));
    }

    public void setBlockInteractionRange(double blockInteractionRange) {
        this.blockInteractionRange = Math.max(0.0, Math.min(64.0, blockInteractionRange));
    }

    public double width() {
        return MotionDefaults.BASE_WIDTH * scale;
    }

    public double height() {
        return MotionDefaults.STANDING_HEIGHT * scale;
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

    public double fallDamageMultiplier() {
        return fallDamageMultiplier;
    }

    public double airDragModifier() {
        return airDragModifier;
    }

    public double frictionModifier() {
        return frictionModifier;
    }

    public double blockBreakSpeed() {
        return blockBreakSpeed;
    }

    public double miningEfficiency() {
        return miningEfficiency;
    }

    public double submergedMiningSpeed() {
        return submergedMiningSpeed;
    }

    public double blockInteractionRange() {
        return blockInteractionRange;
    }

    public void reset() {
        scale = 1.0;
        movementSpeed = MotionDefaults.BASE_MOVEMENT_SPEED;
        jumpStrength = MotionDefaults.JUMP_POWER;
        gravity = MotionDefaults.GRAVITY;
        stepHeight = MotionDefaults.STEP_HEIGHT;
        sneakingSpeed = MotionDefaults.SNEAKING_SPEED;
        movementEfficiency = MotionDefaults.MOVEMENT_EFFICIENCY;
        waterMovementEfficiency = MotionDefaults.WATER_MOVEMENT_EFFICIENCY;
        flyingSpeed = MotionDefaults.FLYING_SPEED;
        safeFallDistance = MotionDefaults.SAFE_FALL_DISTANCE;
        fallDamageMultiplier = 1.0;
        airDragModifier = 1.0;
        frictionModifier = 1.0;
        blockBreakSpeed = 1.0;
        miningEfficiency = 0.0;
        submergedMiningSpeed = 0.2;
        blockInteractionRange = 4.5;
    }
}
