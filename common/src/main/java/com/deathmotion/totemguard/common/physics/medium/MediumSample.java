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

package com.deathmotion.totemguard.common.physics.medium;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class MediumSample {

    private boolean water;
    private boolean lava;
    private boolean climbable;
    private boolean climbableUncertain;
    private boolean stuck;
    private boolean powderSnowSwept;
    private boolean stuckAlongPath;
    private double stuckHorizontal;
    private double stuckVertical;
    private double bubbleAscent;
    private double pushX;
    private double pushY;
    private double pushZ;
    private boolean swimSteerWater;
    private boolean eyeInWater;
    private boolean waterAtFeet;

    private double boxMinX;
    private double boxFeetY;
    private double boxMinZ;
    private double boxMaxX;
    private double boxHeadY;
    private double boxMaxZ;
    private double eyeSampleY;
    private boolean wetCellFound;
    private int wetCellX;
    private int wetCellY;
    private int wetCellZ;
    private double wetCellSurface;
    private int fluidCellX0;
    private int fluidCellX1;
    private int fluidCellY0;
    private int fluidCellY1;
    private int fluidCellZ0;
    private int fluidCellZ1;

    public void reset() {
        boxMinX = boxFeetY = boxMinZ = 0.0;
        boxMaxX = boxHeadY = boxMaxZ = 0.0;
        eyeSampleY = 0.0;
        wetCellFound = false;
        wetCellX = wetCellY = wetCellZ = 0;
        wetCellSurface = 0.0;
        fluidCellX0 = fluidCellX1 = 0;
        fluidCellY0 = fluidCellY1 = 0;
        fluidCellZ0 = fluidCellZ1 = 0;
        water = false;
        lava = false;
        climbable = false;
        climbableUncertain = false;
        stuck = false;
        powderSnowSwept = false;
        stuckAlongPath = false;
        stuckHorizontal = 1.0;
        stuckVertical = 1.0;
        bubbleAscent = 0.0;
        pushX = 0.0;
        pushY = 0.0;
        pushZ = 0.0;
        swimSteerWater = false;
        eyeInWater = false;
        waterAtFeet = false;
    }

    public boolean pushed() {
        return pushX != 0.0 || pushY != 0.0 || pushZ != 0.0;
    }

    public boolean fluid() {
        return water || lava;
    }

    public boolean landMedium() {
        return !fluid() && !stuck && !climbable;
    }
}
