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

    public void reset() {
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
