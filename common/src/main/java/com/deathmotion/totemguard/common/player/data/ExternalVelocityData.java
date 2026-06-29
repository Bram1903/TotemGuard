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

import com.deathmotion.totemguard.common.player.movement.Interval;

public class ExternalVelocityData {

    private static final int HOLD_TICKS = 4;

    private Interval x = Interval.ZERO;
    private Interval z = Interval.ZERO;
    private int holdTicks;

    public void addPush(double px, double pz) {
        x = x.hull(0.0).hull(px);
        z = z.hull(0.0).hull(pz);
        holdTicks = HOLD_TICKS;
    }

    public void tick() {
        if (holdTicks <= 0) return;
        if (--holdTicks <= 0) {
            x = Interval.ZERO;
            z = Interval.ZERO;
        }
    }

    public Interval x() {
        return x;
    }

    public Interval z() {
        return z;
    }

    public boolean hasHorizontal() {
        return holdTicks > 0;
    }

    public void reset() {
        x = Interval.ZERO;
        z = Interval.ZERO;
        holdTicks = 0;
    }
}
