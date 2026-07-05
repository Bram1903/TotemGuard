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

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.PendingBlocks;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.deathmotion.totemguard.common.world.shape.ShapeRegistry;

public final class ColliderCollector {

    private ColliderCollector() {
    }

    public static void fill(ColliderBuffer buffer, BlockReader reader, EntityTracker entities,
                            ShapeQuery query, ExemptCells exempt,
                            double minX, double minY, double minZ,
                            double maxX, double maxY, double maxZ) {
        buffer.reset();
        int x0 = floor(minX), x1 = floor(maxX);
        int y0 = floor(minY), y1 = floor(maxY);
        int z0 = floor(minZ), z1 = floor(maxZ);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    int clientId = reader.stateId(x, y, z);
                    long facts = reader.factsForClientId(clientId);
                    boolean uncertain = reader.uncertain(x, y, z);
                    boolean collectible = StateFacts.is(facts, StateFacts.HAS_SHAPE | StateFacts.SUPPORT_APPROXIMATE);
                    if (!collectible && !uncertain) continue;

                    long trust = (uncertain ? ColliderBuffer.TAG_UNCERTAIN : 0L)
                            | (exempt.contains(x, y, z) ? ColliderBuffer.TAG_EXEMPT : 0L);
                    if (collectible) {
                        buffer.tag(facts | ColliderBuffer.KIND_BLOCK | trust);
                        ShapeRegistry.collect(reader.stateForClientId(clientId), x, y, z, query, buffer);
                    }
                    if (uncertain) {
                        int pendingId = reader.pendingStateId(x, y, z);
                        if (pendingId != PendingBlocks.NONE && pendingId != clientId) {
                            long pendingFacts = reader.factsForClientId(pendingId);
                            if (StateFacts.is(pendingFacts, StateFacts.HAS_SHAPE | StateFacts.SUPPORT_APPROXIMATE)) {
                                buffer.tag(pendingFacts | ColliderBuffer.KIND_BLOCK | trust);
                                ShapeRegistry.collect(reader.stateForClientId(pendingId), x, y, z, query, buffer);
                            }
                        }
                    }
                }
            }
        }

        // Entity positions carry interpolation uncertainty, so their boxes support but never clip:
        // charging a crossing against a box the client may render elsewhere would be a false phase.
        buffer.tag(ColliderBuffer.KIND_ENTITY | ColliderBuffer.TAG_UNCERTAIN);
        entities.collectStandable(minX, minY, minZ, maxX, maxY, maxZ, buffer);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
