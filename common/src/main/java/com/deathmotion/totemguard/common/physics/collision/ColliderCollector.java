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

import com.deathmotion.totemguard.common.player.data.PistonData;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.PendingBlocks;
import com.deathmotion.totemguard.common.world.block.PredictedBlocks;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.deathmotion.totemguard.common.world.shape.ShapeRegistry;

public final class ColliderCollector {

    private ColliderCollector() {
    }

    public static void fill(ColliderBuffer buffer, BlockReader reader, EntityTracker entities,
                            ShapeQuery query, ExemptCells exempt, PistonData pistons, int excludeEntityId,
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
                        buffer.cell(ColliderBuffer.packCell(x, y, z));
                        collectShape(buffer, reader, query, clientId, facts, x, y, z);
                    }
                    if (uncertain) {
                        int pendingId = reader.pendingStateId(x, y, z);
                        if (pendingId != PendingBlocks.NONE && pendingId != clientId) {
                            long pendingFacts = reader.factsForClientId(pendingId);
                            if (StateFacts.is(pendingFacts, StateFacts.HAS_SHAPE | StateFacts.SUPPORT_APPROXIMATE)) {
                                buffer.tag(pendingFacts | ColliderBuffer.KIND_BLOCK | trust);
                                buffer.cell(ColliderBuffer.packCell(x, y, z));
                                collectShape(buffer, reader, query, pendingId, pendingFacts, x, y, z);
                            }
                        }
                        int predictedId = reader.predictedStateId(x, y, z);
                        if (predictedId != PredictedBlocks.NONE && predictedId != clientId && predictedId != pendingId) {
                            long predictedFacts = reader.factsForClientId(predictedId);
                            if (StateFacts.is(predictedFacts, StateFacts.HAS_SHAPE | StateFacts.SUPPORT_APPROXIMATE)) {
                                buffer.tag(predictedFacts | ColliderBuffer.KIND_BLOCK | trust);
                                buffer.cell(ColliderBuffer.packCell(x, y, z));
                                buffer.accept(x, y, z, x + 1.0, y + 1.0, z + 1.0);
                            }
                        }
                    }
                }
            }
        }

        // Entity positions carry interpolation uncertainty, so their boxes support but never clip:
        // charging a crossing against a box the client may render elsewhere would be a false phase.
        // excludeEntityId drops the ridden vehicle itself so it never reads as its own support.
        buffer.tag(ColliderBuffer.KIND_ENTITY | ColliderBuffer.TAG_UNCERTAIN);
        buffer.cell(ColliderBuffer.NO_CELL);
        entities.collectStandable(minX, minY, minZ, maxX, maxY, maxZ, buffer, excludeEntityId);

        collectMovingPistons(buffer, pistons, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void collectShape(ColliderBuffer buffer, BlockReader reader, ShapeQuery query,
                                    int clientId, long facts, int x, int y, int z) {
        if ((facts & (StateFacts.FULL_CUBE | StateFacts.SUPPORT_APPROXIMATE)) == StateFacts.FULL_CUBE) {
            buffer.accept(x, y, z, x + 1.0, y + 1.0, z + 1.0);
            return;
        }
        ShapeRegistry.collect(reader.stateForClientId(clientId), x, y, z, query, buffer);
    }

    private static void collectMovingPistons(ColliderBuffer buffer, PistonData pistons,
                                             double minX, double minY, double minZ,
                                             double maxX, double maxY, double maxZ) {
        if (!pistons.isActive()) return;
        buffer.tag(ColliderBuffer.KIND_BLOCK | ColliderBuffer.TAG_UNCERTAIN | StateFacts.SUPPORT_APPROXIMATE);
        buffer.cell(ColliderBuffer.NO_CELL);
        for (int i = 0; i < pistons.sceneCount(); i++) {
            PistonData.Scene scene = pistons.scene(i);
            if (scene.cellsExact()) {
                for (int c = 0; c < scene.cellCount(); c++) {
                    long cell = scene.cell(c);
                    double bMinX = PistonData.Scene.cellX(cell);
                    double bMinY = PistonData.Scene.cellY(cell);
                    double bMinZ = PistonData.Scene.cellZ(cell);
                    if (bMinX + 1.0 < minX || bMinX > maxX || bMinY + 1.0 < minY || bMinY > maxY
                            || bMinZ + 1.0 < minZ || bMinZ > maxZ) {
                        continue;
                    }
                    buffer.accept(bMinX, bMinY, bMinZ, bMinX + 1.0, bMinY + 1.0, bMinZ + 1.0);
                }
                continue;
            }
            double bMinX = scene.minX();
            double bMinY = scene.minY();
            double bMinZ = scene.minZ();
            double bMaxX = scene.maxX() + 1.0;
            double bMaxY = scene.maxY() + 1.0;
            double bMaxZ = scene.maxZ() + 1.0;
            if (bMaxX < minX || bMinX > maxX || bMaxY < minY || bMinY > maxY
                    || bMaxZ < minZ || bMinZ > maxZ) {
                continue;
            }
            buffer.accept(bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ);
        }
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
