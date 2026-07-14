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

import com.deathmotion.totemguard.common.physics.area.AreaBounds;

// Vanilla zeroes the velocity at the NEXT move's start, so a stuck tick advances to rest.
public final class StuckFactor {

    private static final int ARREST_TICKS = 2;

    private int arrestTicks;

    public void apply(AreaBounds bounds, MediumSample sample) {
        double horizontal = sample.stuckHorizontal();
        bounds.centerX(bounds.centerX() * horizontal);
        bounds.centerZ(bounds.centerZ() * horizontal);
        bounds.radius(bounds.radius() * horizontal);
        double vertical = sample.stuckVertical();
        bounds.ceiling(Math.max(0.0, bounds.ceiling()) * vertical);
        bounds.floor(bounds.floor() * vertical);
    }

    public void applyArrestWindow(AreaBounds bounds) {
        if (arrestTicks > 0) bounds.raiseCeiling(0.0);
    }

    public void advanceWindow(boolean stuckAlongPath) {
        arrestTicks = stuckAlongPath ? ARREST_TICKS : Math.max(0, arrestTicks - 1);
    }

    public void reset() {
        arrestTicks = 0;
    }
}
