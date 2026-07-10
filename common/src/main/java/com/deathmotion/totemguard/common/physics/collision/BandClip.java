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

package com.deathmotion.totemguard.common.physics.collision;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BandClip {

    private double floor;
    private double ceiling;

    public void clipY(ColliderBuffer colliders,
                      double minX, double minY, double minZ,
                      double maxX, double maxY, double maxZ,
                      double lo, double hi) {
        double clipLo = AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, minY, minZ, maxX, maxY, maxZ, lo, true);
        double clipHi = hi == lo ? clipLo
                : AxisClip.clip(colliders, AxisClip.AXIS_Y,
                minX, minY, minZ, maxX, maxY, maxZ, hi, true);
        floor = Math.min(clipLo, clipHi);
        ceiling = Math.max(clipLo, clipHi);
    }
}
