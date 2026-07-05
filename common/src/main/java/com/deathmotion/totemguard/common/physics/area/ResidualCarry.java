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
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class ResidualCarry {

    private double horizontal;
    private double vertical;

    public void store(double missHorizontal, double missVertical, boolean detectionTick, double cap) {
        if (detectionTick || cap <= 0.0) {
            clear();
            return;
        }
        horizontal = Math.min(cap, Math.max(0.0, missHorizontal));
        vertical = Math.min(cap, Math.max(0.0, missVertical));
    }

    public void clear() {
        horizontal = 0.0;
        vertical = 0.0;
    }
}
