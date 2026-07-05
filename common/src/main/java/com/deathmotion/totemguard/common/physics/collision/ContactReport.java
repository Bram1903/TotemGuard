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

package com.deathmotion.totemguard.common.physics.collision;

import com.deathmotion.totemguard.common.world.block.BlockTraits;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class ContactReport {

    public static final double NO_SUPPORT = Double.MAX_VALUE;

    private double allowedX;
    private double allowedY;
    private double allowedZ;
    private boolean collidedX;
    private boolean collidedY;
    private boolean collidedZ;
    private boolean groundHit;
    private boolean ceilingHit;
    private double stepUsedHeight;
    private double crossX;
    private double crossY;
    private double crossZ;

    private boolean startOverlapping;
    private boolean startSuffocating;
    private double embedDepth;

    private double supportGap;
    private double trailingSupportGap;
    private double supportTop;
    private boolean supportIsEntity;
    private boolean supportApproximate;
    private double supportSlipMin;
    private double supportSlipMax;
    private double supportBounce;
    private double supportSpeedFactor;
    private double supportJumpFactor;

    private double ceilingClearance;
    private boolean wallNear;

    public void reset() {
        allowedX = 0.0;
        allowedY = 0.0;
        allowedZ = 0.0;
        collidedX = false;
        collidedY = false;
        collidedZ = false;
        groundHit = false;
        ceilingHit = false;
        stepUsedHeight = 0.0;
        crossX = 0.0;
        crossY = 0.0;
        crossZ = 0.0;
        startOverlapping = false;
        startSuffocating = false;
        embedDepth = 0.0;
        supportGap = NO_SUPPORT;
        trailingSupportGap = NO_SUPPORT;
        supportTop = Double.NEGATIVE_INFINITY;
        supportIsEntity = false;
        supportApproximate = false;
        supportSlipMin = BlockTraits.DEFAULT_SLIPPERINESS;
        supportSlipMax = BlockTraits.DEFAULT_SLIPPERINESS;
        supportBounce = 0.0;
        supportSpeedFactor = 1.0;
        supportJumpFactor = 1.0;
        ceilingClearance = Double.MAX_VALUE;
        wallNear = false;
    }

    public double nearestSupportGap() {
        return Math.min(supportGap, trailingSupportGap);
    }

    public double crossingDepth() {
        return Math.max(Math.abs(crossX), Math.max(Math.abs(crossY), Math.abs(crossZ)));
    }

    public double horizontalCrossingDepth() {
        return Math.max(Math.abs(crossX), Math.abs(crossZ));
    }
}
