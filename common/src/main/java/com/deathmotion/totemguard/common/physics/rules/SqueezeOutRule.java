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

package com.deathmotion.totemguard.common.physics.rules;

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.deathmotion.totemguard.common.world.shape.ShapeRegistry;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class SqueezeOutRule {

    private static final double NUDGE = 0.1;

    private static final double CORNER_SCALE = 0.7;
    private static final double DEFLATE = 1.0e-7;
    private static final int[] SCAN_X = {-1, 1, 0, 0};
    private static final int[] SCAN_Z = {0, 0, -1, 1};

    private boolean setX;
    private boolean setZ;
    private double valX;
    private double valZ;

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public boolean evaluate(BlockReader reader, ShapeQuery query,
                            double x, double feetY, double z,
                            double halfWidth, double height) {
        setX = false;
        setZ = false;
        valX = 0.0;
        valZ = 0.0;
        double off = halfWidth * CORNER_SCALE;
        corner(reader, query, x - off, feetY, z + off, height);
        corner(reader, query, x - off, feetY, z - off, height);
        corner(reader, query, x + off, feetY, z - off, height);
        corner(reader, query, x + off, feetY, z + off, height);
        return setX || setZ;
    }

    private void corner(BlockReader reader, ShapeQuery query,
                        double cornerX, double feetY, double cornerZ, double height) {
        int cellX = floor(cornerX);
        int cellZ = floor(cornerZ);
        if (!suffocates(reader, query, cellX, cellZ, feetY, feetY + height)) return;
        double dx = cornerX - cellX;
        double dz = cornerZ - cellZ;
        int bestFace = -1;
        double best = Double.MAX_VALUE;
        for (int face = 0; face < 4; face++) {
            double along = SCAN_X[face] != 0 ? dx : dz;
            double distance = (SCAN_X[face] > 0 || SCAN_Z[face] > 0) ? 1.0 - along : along;
            if (distance < best
                    && !suffocates(reader, query, cellX + SCAN_X[face], cellZ + SCAN_Z[face],
                    feetY, feetY + height)) {
                best = distance;
                bestFace = face;
            }
        }
        if (bestFace < 0) return;
        if (SCAN_X[bestFace] != 0) {
            setX = true;
            valX = NUDGE * SCAN_X[bestFace];
        } else {
            setZ = true;
            valZ = NUDGE * SCAN_Z[bestFace];
        }
    }

    private boolean suffocates(BlockReader reader, ShapeQuery query,
                               int cellX, int cellZ, double minY, double maxY) {
        double boxMinX = cellX + DEFLATE;
        double boxMaxX = cellX + 1.0 - DEFLATE;
        double boxMinY = minY + DEFLATE;
        double boxMaxY = maxY - DEFLATE;
        double boxMinZ = cellZ + DEFLATE;
        double boxMaxZ = cellZ + 1.0 - DEFLATE;
        int y0 = floor(boxMinY);
        int y1 = floor(boxMaxY);
        boolean[] hit = {false};
        for (int y = y0; y <= y1 && !hit[0]; y++) {
            if (!StateFacts.is(reader.facts(cellX, y, cellZ), StateFacts.SUFFOCATING)) continue;
            ShapeRegistry.collect(reader.state(cellX, y, cellZ), cellX, y, cellZ, query,
                    (bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ) -> {
                        if (bMaxX > boxMinX && bMinX < boxMaxX
                                && bMaxY > boxMinY && bMinY < boxMaxY
                                && bMaxZ > boxMinZ && bMinZ < boxMaxZ) {
                            hit[0] = true;
                        }
                    });
        }
        return hit[0];
    }
}
