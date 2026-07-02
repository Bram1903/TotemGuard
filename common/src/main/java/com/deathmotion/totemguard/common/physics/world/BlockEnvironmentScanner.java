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

package com.deathmotion.totemguard.common.physics.world;

import com.deathmotion.totemguard.common.player.data.ClientWorld;
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.util.BoundingBox;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.Set;

import static com.deathmotion.totemguard.common.physics.MovementConstants.BUBBLE_COLUMN_INSIDE_ASCENT;
import static com.deathmotion.totemguard.common.physics.MovementConstants.BUBBLE_COLUMN_SURFACE_ASCENT;

public final class BlockEnvironmentScanner {

    private static final double SUPPORT_BLOCK_OFFSET = 0.5000001;
    private static final double SUPPORT_TOP_EPS = 0.001;
    private static final double UNSUPPORTED_GAP = 10.0;
    private static final double FLUID_SURFACE_MARGIN = 0.01;
    private static final double CEILING_SCAN_REACH = 2.0;
    private static final double WALL_CONTACT_EPS = 1.0e-7;

    private BlockEnvironmentScanner() {
    }

    public static BlockEnvironment scan(ClientWorld world, WorldEntityData entities, Location current, Location previous,
                                        double width, double poseHeight, double stepHeight, boolean sneaking,
                                        Set<Long> wallExemptCells) {
        if (!world.isLoaded(floor(current.getX()) >> 4, floor(current.getZ()) >> 4)) {
            return BlockEnvironment.UNLOADED;
        }

        BoundingBox startBody = BoundingBox.player(previous, width, poseHeight);
        boolean fluid = fluidReachesFeet(world, startBody, previous.getY());
        double bubbleAscent = bubbleColumnAscent(world, BoundingBox.sweptPlayer(current, previous, width, poseHeight));

        Stuck stuck = scanStuck(world, previous, width, poseHeight);
        boolean climbable = climbableAt(world, previous);
        CollisionContext ctx = new CollisionContext(current.getY(), sneaking);
        Below below = scanBelow(world, entities, current, previous, width, ctx);
        WallGaps wallGaps = scanWalls(world, current, previous, width / 2.0, stepHeight, ctx, wallExemptCells);
        double ceilingGap = scanCeilingGap(world, current, previous, width / 2.0, poseHeight, ctx, wallExemptCells);
        int overlapState = bodyOverlapState(world, startBody, ctx);
        return new BlockEnvironment(true, fluid, climbable, stuck.active, stuck.horizontal, stuck.vertical,
                below.bounceFactor, below.slipperinessMin, below.slipperinessMax, below.blockSpeedFactor, below.groundGap,
                bubbleAscent, wallGaps, ceilingGap,
                (overlapState & OVERLAP_SUFFOCATING) != 0, overlapState != 0);
    }

    static final int OVERLAP_ANY = 1;
    static final int OVERLAP_SUFFOCATING = 2;

    private static int bodyOverlapState(ClientWorld world, BoundingBox body, CollisionContext ctx) {
        double feetY = body.minY();
        int state = 0;
        for (int x = floor(body.minX()); x <= floor(body.maxX()); x++) {
            for (int y = floor(body.minY()); y <= floor(body.maxY()); y++) {
                for (int z = floor(body.minZ()); z <= floor(body.maxZ()); z++) {
                    WrappedBlockState blockState = world.getBlockState(x, y, z);
                    CollisionShape shape = BlockShapes.shapeOf(blockState, y, ctx);
                    if (shape.isEmpty()) continue;
                    boolean suffocating = shape.isFullCube() || BlockShapes.suffocatingOverride(blockState.getType());
                    if ((state & OVERLAP_ANY) != 0 && !suffocating) continue;
                    for (CollisionBox box : shape.boxes()) {
                        if (x + box.maxX() <= body.minX() + WALL_CONTACT_EPS || x + box.minX() >= body.maxX() - WALL_CONTACT_EPS) continue;
                        if (y + box.maxY() <= body.minY() + WALL_CONTACT_EPS || y + box.minY() >= body.maxY() - WALL_CONTACT_EPS) continue;
                        if (z + box.maxZ() <= body.minZ() + WALL_CONTACT_EPS || z + box.minZ() >= body.maxZ() - WALL_CONTACT_EPS) continue;
                        if (y + box.maxY() - feetY <= SUPPORT_TOP_EPS) continue;
                        state |= suffocating ? (OVERLAP_ANY | OVERLAP_SUFFOCATING) : OVERLAP_ANY;
                        break;
                    }
                    if ((state & OVERLAP_SUFFOCATING) != 0) return state;
                }
            }
        }
        return state;
    }

    private static double scanCeilingGap(ClientWorld world, Location current, Location previous, double half,
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
                    CollisionShape shape = BlockShapes.shapeOf(state, y, ctx);
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

    private static WallGaps scanWalls(ClientWorld world, Location current, Location previous, double half,
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
                    CollisionShape shape = BlockShapes.shapeOf(state, y, ctx);
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

    private static double bubbleColumnAscent(ClientWorld world, BoundingBox body) {
        int x0 = floor(body.minX()), x1 = floor(body.maxX());
        int y0 = floor(body.minY()), y1 = floor(body.maxY());
        int z0 = floor(body.minZ()), z1 = floor(body.maxZ());
        double cap = 0.0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    WrappedBlockState state = world.getBlockState(x, y, z);
                    if (!MovementBlocks.isBubbleColumn(state.getType()) || state.isDrag()) continue;
                    boolean surface = !MovementBlocks.isBubbleColumn(world.getBlockState(x, y + 1, z).getType());
                    cap = Math.max(cap, surface ? BUBBLE_COLUMN_SURFACE_ASCENT : BUBBLE_COLUMN_INSIDE_ASCENT);
                }
            }
        }
        return cap;
    }

    private static boolean fluidReachesFeet(ClientWorld world, BoundingBox body, double feetY) {
        int x0 = floor(body.minX()), x1 = floor(body.maxX());
        int y0 = floor(body.minY()), y1 = floor(body.maxY());
        int z0 = floor(body.minZ()), z1 = floor(body.maxZ());
        double threshold = feetY - FLUID_SURFACE_MARGIN;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    WrappedBlockState state = world.getBlockState(x, y, z);
                    if (!MovementBlocks.isFluid(state)) continue;
                    double height = MovementBlocks.isFluid(world.getBlockState(x, y + 1, z))
                            ? 1.0
                            : MovementBlocks.fluidSurfaceHeight(state);
                    if (y + height >= threshold) return true;
                }
            }
        }
        return false;
    }

    private static Stuck scanStuck(ClientWorld world, Location feet, double width, double poseHeight) {
        BoundingBox body = BoundingBox.player(feet, width, poseHeight);
        double[] best = {-1.0, 1.0, 1.0};
        world.forEachBlock(body, state -> {
            StateType type = state.getType();
            if (MovementBlocks.isStuck(type)) {
                double h = MovementBlocks.stuckHorizontal(type);
                if (h > best[0]) {
                    best[0] = h;
                    best[1] = h;
                    best[2] = MovementBlocks.stuckVertical(type);
                }
            }
            return false;
        });
        return new Stuck(best[0] >= 0.0, best[1], best[2]);
    }

    private static boolean climbableAt(ClientWorld world, Location feet) {
        int x = floor(feet.getX()), y = floor(feet.getY()), z = floor(feet.getZ());
        WrappedBlockState state = world.getBlockState(x, y, z);
        if (MovementBlocks.isClimbable(state.getType())) return true;
        return MovementBlocks.trapdoorUsableAsLadder(state, world.getBlockState(x, y - 1, z));
    }

    private static Below scanBelow(ClientWorld world, WorldEntityData entities, Location feet, Location previous, double width, CollisionContext ctx) {
        double half = width / 2.0;
        double feetYd = feet.getY();
        int feetCell = floor(feetYd);

        double currentTop = scanSupportTop(world, feet.getX(), feet.getZ(), half, feetYd, feetCell, ctx);
        double previousTop = scanSupportTop(world, previous.getX(), previous.getZ(), half, feetYd, feetCell, ctx);
        boolean usePrevious = previousTop > currentTop;
        double supportTop = usePrevious ? previousTop : currentTop;
        double winX = usePrevious ? previous.getX() : feet.getX();
        double winZ = usePrevious ? previous.getZ() : feet.getZ();

        double bounceFactor = scanBounce(world, winX, winZ, half, feetYd);
        double[] slip = scanSlipRange(world, winX, winZ, half, feetYd);
        double slipperinessMin = slip[0];
        double slipperinessMax = slip[1];
        double blockSpeedFactor = feetBlockSpeedFactor(world, feet, half);

        double entityTop = entitySupportTop(entities, feet, previous, half, feetYd);
        if (entityTop > supportTop) {
            supportTop = entityTop;
            slipperinessMin = MovementBlocks.slipperiness(StateTypes.AIR);
            slipperinessMax = slipperinessMin;
            bounceFactor = 0.0;
            blockSpeedFactor = 1.0;
        }

        double groundGap = supportTop == Double.NEGATIVE_INFINITY ? UNSUPPORTED_GAP : feetYd - supportTop;
        return new Below(bounceFactor, slipperinessMin, slipperinessMax, blockSpeedFactor, groundGap);
    }

    private static double feetBlockSpeedFactor(ClientWorld world, Location feet, double half) {
        double factor = Double.NEGATIVE_INFINITY;
        int fy = floor(feet.getY());
        int by = floor(feet.getY() - SUPPORT_BLOCK_OFFSET);
        for (double[] point : footprint(feet.getX(), feet.getZ(), half)) {
            int px = floor(point[0]), pz = floor(point[1]);
            double at = MovementBlocks.blockSpeedFactor(world.getBlockState(px, fy, pz).getType());
            double below = at != 1.0 ? at : MovementBlocks.blockSpeedFactor(world.getBlockState(px, by, pz).getType());
            factor = Math.max(factor, below);
        }
        return factor;
    }

    private static double entitySupportTop(WorldEntityData entities, Location feet, Location previous, double half, double feetYd) {
        if (entities == null) return Double.NEGATIVE_INFINITY;
        double minX = Math.min(feet.getX(), previous.getX()) - half;
        double maxX = Math.max(feet.getX(), previous.getX()) + half;
        double minZ = Math.min(feet.getZ(), previous.getZ()) - half;
        double maxZ = Math.max(feet.getZ(), previous.getZ()) + half;
        return entities.standableSupportTop(minX, minZ, maxX, maxZ, feetYd);
    }

    private static double scanBounce(ClientWorld world, double cx, double cz, double half, double feetYd) {
        int y = floor(feetYd - SUPPORT_BLOCK_OFFSET);
        double bounceFactor = 0.0;
        for (double[] point : footprint(cx, cz, half)) {
            StateType nearType = world.getBlockState(floor(point[0]), y, floor(point[1])).getType();
            bounceFactor = Math.max(bounceFactor, MovementBlocks.bounceFactor(nearType));
        }
        return bounceFactor;
    }

    private static double scanSupportTop(ClientWorld world, double cx, double cz, double half, double feetYd, int feetCell, CollisionContext ctx) {
        double minX = cx - half, maxX = cx + half;
        double minZ = cz - half, maxZ = cz + half;
        double best = Double.NEGATIVE_INFINITY;
        for (int px = floor(minX); px <= floor(maxX); px++) {
            for (int pz = floor(minZ); pz <= floor(maxZ); pz++) {
                for (int cellY = feetCell; cellY >= feetCell - 2; cellY--) {
                    WrappedBlockState state = world.getBlockState(px, cellY, pz);
                    CollisionShape shape = BlockShapes.shapeOf(state, cellY, ctx);
                    if (shape.isEmpty()) continue;
                    double cap = feetYd - cellY + SUPPORT_TOP_EPS;
                    double top = BlockShapes.supportApproximate(state.getType())
                            ? shape.supportTopClamped(cap, minX - px, maxX - px, minZ - pz, maxZ - pz)
                            : shape.supportTop(cap, minX - px, maxX - px, minZ - pz, maxZ - pz);
                    if (top == Double.NEGATIVE_INFINITY) continue;
                    double topAbs = Math.min(cellY + top, feetYd);
                    if (topAbs > best) best = topAbs;
                }
            }
        }
        return best;
    }

    private static double[] scanSlipRange(ClientWorld world, double cx, double cz, double half, double feetYd) {
        int y = floor(feetYd - SUPPORT_BLOCK_OFFSET);
        double slipMin = Double.POSITIVE_INFINITY;
        double slipMax = Double.NEGATIVE_INFINITY;
        for (double[] point : footprint(cx, cz, half)) {
            double slip = MovementBlocks.slipperiness(world.getBlockState(floor(point[0]), y, floor(point[1])).getType());
            slipMin = Math.min(slipMin, slip);
            slipMax = Math.max(slipMax, slip);
        }
        return new double[]{slipMin, slipMax};
    }

    private static double[][] footprint(double cx, double cz, double half) {
        return new double[][]{{cx - half, cz - half}, {cx + half, cz - half}, {cx - half, cz + half}, {cx + half, cz + half}, {cx, cz}};
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private record Below(double bounceFactor, double slipperinessMin, double slipperinessMax, double blockSpeedFactor, double groundGap) {
    }

    private record Stuck(boolean active, double horizontal, double vertical) {
    }
}
