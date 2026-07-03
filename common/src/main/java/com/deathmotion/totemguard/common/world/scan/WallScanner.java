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

package com.deathmotion.totemguard.common.world.scan;

import com.deathmotion.totemguard.common.player.data.ClientWorld;
import com.deathmotion.totemguard.common.util.BoundingBox;
import com.deathmotion.totemguard.common.world.collisions.BlockShapes;
import com.deathmotion.totemguard.common.world.collisions.CollisionBox;
import com.deathmotion.totemguard.common.world.collisions.CollisionContext;
import com.deathmotion.totemguard.common.world.collisions.CollisionShape;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.Set;

import static com.deathmotion.totemguard.common.world.scan.Scans.floor;

final class WallScanner {

    static final int OVERLAP_ANY = 1;
    static final int OVERLAP_SUFFOCATING = 2;

    private static final double CEILING_SCAN_REACH = 2.0;
    private static final double WALL_CONTACT_EPS = 1.0e-7;

    private WallScanner() {
    }

    static int overlapState(ClientWorld world, BoundingBox body, CollisionContext ctx) {
        double feetY = body.minY();
        int state = 0;
        for (int x = floor(body.minX()); x <= floor(body.maxX()); x++) {
            for (int y = floor(body.minY()); y <= floor(body.maxY()); y++) {
                for (int z = floor(body.minZ()); z <= floor(body.maxZ()); z++) {
                    WrappedBlockState blockState = world.getBlockState(x, y, z);
                    CollisionShape shape = BlockShapes.shapeOf(blockState, x, y, z, ctx);
                    if (shape.isEmpty()) continue;
                    boolean suffocating = (shape.isFullCube() && !BlockShapes.suffocatingNever(blockState.getType()))
                            || BlockShapes.suffocatingOverride(blockState.getType());
                    if ((state & OVERLAP_ANY) != 0 && !suffocating) continue;
                    for (CollisionBox box : shape.boxes()) {
                        if (x + box.maxX() <= body.minX() + WALL_CONTACT_EPS || x + box.minX() >= body.maxX() - WALL_CONTACT_EPS) continue;
                        if (y + box.maxY() <= body.minY() + WALL_CONTACT_EPS || y + box.minY() >= body.maxY() - WALL_CONTACT_EPS) continue;
                        if (z + box.maxZ() <= body.minZ() + WALL_CONTACT_EPS || z + box.minZ() >= body.maxZ() - WALL_CONTACT_EPS) continue;
                        if (y + box.maxY() - feetY <= Scans.SUPPORT_TOP_EPS) continue;
                        state |= suffocating ? (OVERLAP_ANY | OVERLAP_SUFFOCATING) : OVERLAP_ANY;
                        break;
                    }
                    if ((state & OVERLAP_SUFFOCATING) != 0) return state;
                }
            }
        }
        return state;
    }

    static double ceilingGap(ClientWorld world, Location current, Location previous, double half,
                             double poseHeight, CollisionContext ctx, Set<Long> exemptCells) {
        double headY = previous.getY() + poseHeight;
        double minX = Math.min(previous.getX(), current.getX()) - half;
        double maxX = Math.max(previous.getX(), current.getX()) + half;
        double minZ = Math.min(previous.getZ(), current.getZ()) - half;
        double maxZ = Math.max(previous.getZ(), current.getZ()) + half;

        double gap = Double.POSITIVE_INFINITY;
        int y0 = floor(headY);
        int y1 = floor(headY + CEILING_SCAN_REACH);
        for (int x = floor(minX); x <= floor(maxX); x++) {
            for (int z = floor(minZ); z <= floor(maxZ); z++) {
                for (int y = y0; y <= y1; y++) {
                    if (!exemptCells.isEmpty() && exemptCells.contains(ClientWorld.blockKey(x, y, z))) continue;
                    WrappedBlockState state = world.getBlockState(x, y, z);
                    CollisionShape shape = BlockShapes.shapeOf(state, x, y, z, ctx);
                    if (shape.isEmpty()) continue;
                    for (CollisionBox box : shape.boxes()) {
                        if (y + box.maxY() <= headY) continue;
                        double bx0 = x + box.minX(), bx1 = x + box.maxX();
                        double bz0 = z + box.minZ(), bz1 = z + box.maxZ();
                        if (bx1 <= minX + WALL_CONTACT_EPS || bx0 >= maxX - WALL_CONTACT_EPS) continue;
                        if (bz1 <= minZ + WALL_CONTACT_EPS || bz0 >= maxZ - WALL_CONTACT_EPS) continue;
                        double d = Math.max(0.0, y + box.minY() - headY);
                        if (d < gap) gap = d;
                    }
                }
            }
        }
        return gap;
    }

    static WallGaps walls(ClientWorld world, Location current, Location previous, double half,
                          double stepHeight, CollisionContext ctx, Set<Long> exemptCells) {
        double bandMin = current.getY() + stepHeight;

        double sMinX = previous.getX() - half, sMaxX = previous.getX() + half;
        double sMinZ = previous.getZ() - half, sMaxZ = previous.getZ() + half;
        double eMinX = current.getX() - half, eMaxX = current.getX() + half;
        double eMinZ = current.getZ() - half, eMaxZ = current.getZ() + half;

        int x0 = floor(Math.min(sMinX, eMinX)), x1 = floor(Math.max(sMaxX, eMaxX));
        int z0 = floor(Math.min(sMinZ, eMinZ)), z1 = floor(Math.max(sMaxZ, eMaxZ));
        int y0 = floor(bandMin) - 1, y1 = floor(bandMin);

        double crossing = 0.0;
        double embedded = 0.0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    WrappedBlockState state = world.getBlockState(x, y, z);
                    StateType type = state.getType();
                    if (type == StateTypes.AIR || !BlockShapes.wallTrusted(type)) continue;
                    CollisionShape shape = BlockShapes.shapeOf(state, x, y, z, ctx);
                    if (shape.isEmpty()) continue;
                    if (world.hasPendingBlock(x, y, z)) continue;
                    if (!exemptCells.isEmpty() && exemptCells.contains(ClientWorld.blockKey(x, y, z))) continue;
                    for (CollisionBox box : shape.boxes()) {
                        double bottom = y + box.minY(), top = y + box.maxY();
                        if (top <= bandMin + WALL_CONTACT_EPS || bottom > bandMin + WALL_CONTACT_EPS) continue;
                        double bx0 = x + box.minX(), bx1 = x + box.maxX();
                        double bz0 = z + box.minZ(), bz1 = z + box.maxZ();

                        boolean zPathStart = overlaps(bz0, bz1, sMinZ, sMaxZ);
                        boolean zPathEnd = overlaps(bz0, bz1, eMinZ, eMaxZ);
                        if (zPathStart && zPathEnd) {
                            if (sMaxX <= bx0 + WALL_CONTACT_EPS) crossing = Math.max(crossing, eMaxX - bx0);
                            if (sMinX >= bx1 - WALL_CONTACT_EPS) crossing = Math.max(crossing, bx1 - eMinX);
                        }
                        boolean xPathStart = overlaps(bx0, bx1, sMinX, sMaxX);
                        boolean xPathEnd = overlaps(bx0, bx1, eMinX, eMaxX);
                        if (xPathStart && xPathEnd) {
                            if (sMaxZ <= bz0 + WALL_CONTACT_EPS) crossing = Math.max(crossing, eMaxZ - bz0);
                            if (sMinZ >= bz1 - WALL_CONTACT_EPS) crossing = Math.max(crossing, bz1 - eMinZ);
                        }

                        double overlapX = Math.min(eMaxX, bx1) - Math.max(eMinX, bx0);
                        double overlapZ = Math.min(eMaxZ, bz1) - Math.max(eMinZ, bz0);
                        if (overlapX > WALL_CONTACT_EPS && overlapZ > WALL_CONTACT_EPS) {
                            embedded = Math.max(embedded, Math.min(overlapX, overlapZ));
                        }
                    }
                }
            }
        }
        return new WallGaps(Math.max(0.0, crossing), Math.max(0.0, embedded));
    }

    private static boolean overlaps(double min0, double max0, double min1, double max1) {
        return max0 > min1 + WALL_CONTACT_EPS && min0 < max1 - WALL_CONTACT_EPS;
    }

    static boolean horizontalObstacle(ClientWorld world, Location current, double half, double poseHeight,
                                      CollisionContext ctx, double margin) {
        double feetY = current.getY();
        double headY = feetY + poseHeight;
        double minX = current.getX() - half - margin, maxX = current.getX() + half + margin;
        double minZ = current.getZ() - half - margin, maxZ = current.getZ() + half + margin;
        for (int x = floor(minX); x <= floor(maxX); x++) {
            for (int z = floor(minZ); z <= floor(maxZ); z++) {
                for (int y = floor(feetY); y <= floor(headY); y++) {
                    CollisionShape shape = BlockShapes.shapeOf(world.getBlockState(x, y, z), x, y, z, ctx);
                    if (shape.isEmpty()) continue;
                    for (CollisionBox box : shape.boxes()) {
                        if (y + box.maxY() <= feetY + WALL_CONTACT_EPS || y + box.minY() >= headY - WALL_CONTACT_EPS) continue;
                        if (x + box.maxX() <= minX || x + box.minX() >= maxX) continue;
                        if (z + box.maxZ() <= minZ || z + box.minZ() >= maxZ) continue;
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
