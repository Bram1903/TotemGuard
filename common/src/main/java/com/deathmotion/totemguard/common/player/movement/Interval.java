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

package com.deathmotion.totemguard.common.player.movement;

public final class Interval {

    public static final Interval ZERO = new Interval(0.0, 0.0);

    private final double lo;
    private final double hi;

    private Interval(double lo, double hi) {
        this.lo = lo;
        this.hi = hi;
    }

    public Interval expand(double pad) {
        return new Interval(lo - pad, hi + pad);
    }

    public Interval shift(double delta) {
        return new Interval(lo + delta, hi + delta);
    }

    public Interval hull(double v) {
        return new Interval(Math.min(lo, v), Math.max(hi, v));
    }

    public double distanceOutside(double v) {
        if (v < lo) return lo - v;
        if (v > hi) return v - hi;
        return 0.0;
    }

    @Override
    public String toString() {
        return String.format("[%.4f,%.4f]", lo, hi);
    }
}
