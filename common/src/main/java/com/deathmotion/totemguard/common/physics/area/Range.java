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

public record Range(double min, double max) {

    public static final Range ZERO = new Range(0.0, 0.0);

    public static Range of(double value) {
        return new Range(value, value);
    }

    public Range expand(double amount) {
        return new Range(min - amount, max + amount);
    }

    public Range grow(double below, double above) {
        return new Range(min - below, max + above);
    }

    public Range raiseCeiling(double newMax) {
        return newMax > max ? new Range(min, newMax) : this;
    }

    public Range clampToNonNegative() {
        return new Range(Math.max(0.0, min), Math.max(0.0, max));
    }

    public double excessAbove(double value) {
        return Math.max(0.0, value - max);
    }
}
