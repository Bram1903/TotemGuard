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

package com.deathmotion.totemguard.common.physics.area;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class AreaBounds {

    private double centerX;
    private double centerZ;
    private double radius;
    private boolean hasAltCenter;
    private double altCenterX;
    private double altCenterZ;
    private boolean hasSegment;
    private double segDirX;
    private double segDirZ;
    private double segMin;
    private double segMax;
    private double segClosestX;
    private double segClosestZ;
    private double ceiling;
    private double floor;
    private double descentSlack;
    private boolean enforceDescentFloor;
    private double riseFloor;

    private double legalX;
    private double legalZ;
    private double legalVy;

    public void reset(MotionArea area) {
        centerX = area.centerX();
        centerZ = area.centerZ();
        radius = area.slack();
        hasAltCenter = false;
        altCenterX = 0.0;
        altCenterZ = 0.0;
        hasSegment = false;
        segDirX = 0.0;
        segDirZ = 0.0;
        segMin = 0.0;
        segMax = 0.0;
        segClosestX = 0.0;
        segClosestZ = 0.0;
        ceiling = area.ceilVy();
        floor = area.floorVy();
        descentSlack = 0.0;
        enforceDescentFloor = false;
        riseFloor = 0.0;
        legalX = 0.0;
        legalZ = 0.0;
        legalVy = 0.0;
    }

    public double judgedFloor() {
        return riseFloor > 0.0 ? riseFloor : floor - descentSlack;
    }

    public void expandRadius(double amount) {
        if (amount > 0.0) radius += amount;
    }

    public void raiseCeiling(double value) {
        if (value > ceiling) ceiling = value;
    }

    public void lowerFloor(double value) {
        if (value < floor) floor = value;
    }

    public void addDescentSlack(double amount) {
        if (amount > 0.0) descentSlack += amount;
    }

    public void altCenter(double x, double z) {
        hasAltCenter = true;
        altCenterX = x;
        altCenterZ = z;
    }

    public void controlSegment(double dirX, double dirZ, double min, double max) {
        hasSegment = true;
        segDirX = dirX;
        segDirZ = dirZ;
        segMin = min;
        segMax = max;
    }
}
