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

package com.deathmotion.totemguard.common.world.shape;

// sneaking mirrors vanilla Entity.isDescending, which is the shift key, not downward motion.
public record ShapeQuery(double feetY, boolean sneaking, boolean standsOnPowderSnow, boolean deepFall) {

    public static final ShapeQuery DEFAULT = new ShapeQuery(0.0, false, false, false);

    private static final double ABOVE_EPS = 1.0e-5;

    public boolean above(int blockY, double shapeTop) {
        return feetY > blockY + shapeTop - ABOVE_EPS;
    }
}
