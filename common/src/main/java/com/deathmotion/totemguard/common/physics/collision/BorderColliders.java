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

import com.deathmotion.totemguard.common.world.border.BorderMirror;

public final class BorderColliders {

    private static final double WALL_DEPTH = 1.0;
    private static final double MIN_REACH = 1.0;

    private BorderColliders() {
    }

    public static void fill(ColliderBuffer buffer, BorderMirror border,
                            double x, double z, double half,
                            double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ) {
        if (!border.isActive()) return;

        double reach = Math.max(2.0 * half, MIN_REACH);
        if (border.distanceToEdge(x, z) >= reach * 2.0 || !border.withinBounds(x, z, reach)) return;

        double westX = Math.floor(border.minX());
        double eastX = Math.ceil(border.maxX());
        double northZ = Math.floor(border.minZ());
        double southZ = Math.ceil(border.maxZ());

        buffer.tag(ColliderBuffer.KIND_BLOCK);
        buffer.cell(ColliderBuffer.NO_CELL);
        buffer.accept(westX - WALL_DEPTH, minY, minZ, westX, maxY, maxZ);
        buffer.accept(eastX, minY, minZ, eastX + WALL_DEPTH, maxY, maxZ);
        buffer.accept(minX, minY, northZ - WALL_DEPTH, maxX, maxY, northZ);
        buffer.accept(minX, minY, southZ, maxX, maxY, southZ + WALL_DEPTH);
    }
}
