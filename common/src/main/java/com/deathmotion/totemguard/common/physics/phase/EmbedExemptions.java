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

package com.deathmotion.totemguard.common.physics.phase;

import com.deathmotion.totemguard.common.physics.collision.ExemptCells;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.PendingBlocks;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.deathmotion.totemguard.common.world.shape.ShapeRegistry;

import java.util.HashSet;
import java.util.Set;

// Cells are exempted by the relocation event that put the player there, never by mere overlap,
// which would be a standing phase bypass.
public final class EmbedExemptions implements ExemptCells {

    private static final double CONTACT_EPS = 1.0e-7;
    private static final int PRUNE_VERTICAL_SLACK = 2;

    private final Set<Long> cells = new HashSet<>();

    private static boolean overlapsBody(BlockReader reader, ShapeQuery query, int x, int y, int z,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        boolean[] overlap = {false};
        ShapeRegistry.collect(reader.state(x, y, z), x, y, z, query,
                (bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ) -> {
                    if (bMaxX > minX + CONTACT_EPS && bMinX < maxX - CONTACT_EPS
                            && bMaxY > minY + CONTACT_EPS && bMinY < maxY - CONTACT_EPS
                            && bMaxZ > minZ + CONTACT_EPS && bMinZ < maxZ - CONTACT_EPS) {
                        overlap[0] = true;
                    }
                });
        return overlap[0];
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return !cells.isEmpty() && cells.contains(PendingBlocks.blockKey(x, y, z));
    }

    public void seedBodyOverlaps(BlockReader reader, ShapeQuery query,
                                 double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ) {
        for (int x = floor(minX); x <= floor(maxX); x++) {
            for (int y = floor(minY); y <= floor(maxY); y++) {
                for (int z = floor(minZ); z <= floor(maxZ); z++) {
                    if (overlapsBody(reader, query, x, y, z, minX, minY, minZ, maxX, maxY, maxZ)) {
                        cells.add(PendingBlocks.blockKey(x, y, z));
                    }
                }
            }
        }
    }

    public void onBlockApplied(BlockReader reader, ShapeQuery query,
                               double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ,
                               int x, int y, int z, int clientStateId) {
        if (x + 1.0 <= minX || x >= maxX || y + 1.0 <= minY || y >= maxY || z + 1.0 <= minZ || z >= maxZ) return;
        boolean[] overlap = {false};
        ShapeRegistry.collect(reader.stateForClientId(clientStateId), x, y, z, query,
                (bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ) -> {
                    if (bMaxX > minX && bMinX < maxX
                            && bMaxY > minY && bMinY < maxY
                            && bMaxZ > minZ && bMinZ < maxZ) {
                        overlap[0] = true;
                    }
                });
        if (overlap[0]) cells.add(PendingBlocks.blockKey(x, y, z));
    }

    public void prune(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (cells.isEmpty()) return;
        int x0 = floor(minX), x1 = floor(maxX);
        int y0 = floor(minY) - PRUNE_VERTICAL_SLACK, y1 = floor(maxY) + PRUNE_VERTICAL_SLACK;
        int z0 = floor(minZ), z1 = floor(maxZ);
        cells.removeIf(key -> {
            int x = (int) (key >> 38);
            int z = (int) (key << 26 >> 38);
            int y = (int) (key << 52 >> 52);
            return x < x0 || x > x1 || y < y0 || y > y1 || z < z0 || z > z1;
        });
    }

    public void clear() {
        cells.clear();
    }
}
