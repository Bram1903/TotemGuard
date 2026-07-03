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

package com.deathmotion.totemguard.common.physics.prescan;

public final class FastDetector {

    public static final double HORIZONTAL_CAP = 10.0;

    private static final int TOLERANCE = 1;

    private int streak;

    public enum Outcome {
        NONE,
        DECLINE,
        FLAG
    }

    public Outcome evaluate(double horizontalSpeed, boolean externalActive) {
        if (horizontalSpeed <= HORIZONTAL_CAP) {
            streak = 0;
            return Outcome.NONE;
        }
        if (externalActive) {
            streak = 0;
            return Outcome.DECLINE;
        }
        streak++;
        return streak <= TOLERANCE ? Outcome.DECLINE : Outcome.FLAG;
    }

    public void reset() {
        streak = 0;
    }
}
