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

package com.deathmotion.totemguard.common.player.movement.world;

import com.deathmotion.totemguard.common.player.data.ClientWorld;
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.util.BoundingBox;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

public final class BlockEnvironmentScanner {

    private static final double SUPPORT_BLOCK_OFFSET = 0.5000001;
    private static final double SUPPORT_TOP_EPS = 0.001;
    private static final double UNSUPPORTED_GAP = 10.0;

    private BlockEnvironmentScanner() {
    }

    public static BlockEnvironment scan(ClientWorld world, WorldEntityData entities, Location current, Location previous, double width, double poseHeight, boolean sneaking) {
        if (!world.isLoaded(floor(current.getX()) >> 4, floor(current.getZ()) >> 4)) {
            return BlockEnvironment.UNLOADED;
        }

        boolean[] fluid = new boolean[1];
        BoundingBox body = BoundingBox.sweptPlayer(current, previous, width, poseHeight);
        world.forEachBlock(body, state -> {
            if (MovementBlocks.isFluid(state)) {
                fluid[0] = true;
                return true;
            }
            return false;
        });

        Stuck stuck = scanStuck(world, previous, width, poseHeight);
        boolean climbable = climbableAt(world, previous);
        Below below = scanBelow(world, entities, current, previous, width, new CollisionContext(current.getY(), sneaking));
        return new BlockEnvironment(true, fluid[0], climbable, stuck.active, stuck.horizontal, stuck.vertical,
                below.bounceFactor, below.slipperiness, below.groundGap);
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

        Support current = scanSupport(world, feet.getX(), feet.getZ(), half, feetYd, feetCell, ctx);
        Support fallback = scanSupport(world, previous.getX(), previous.getZ(), half, feetYd, feetCell, ctx);
        boolean usePrevious = fallback.top > current.top;
        Support support = usePrevious ? fallback : current;

        double bounceFactor = scanBounce(world, usePrevious ? previous.getX() : feet.getX(),
                usePrevious ? previous.getZ() : feet.getZ(), half, feetYd);

        double supportTop = support.top;
        double slipperiness = support.slipperiness;

        double entityTop = entitySupportTop(entities, feet, previous, half, feetYd);
        if (entityTop > supportTop) {
            supportTop = entityTop;
            slipperiness = MovementBlocks.slipperiness(StateTypes.AIR);
            bounceFactor = 0.0;
        }

        double groundGap = supportTop == Double.NEGATIVE_INFINITY ? UNSUPPORTED_GAP : feetYd - supportTop;
        return new Below(bounceFactor, slipperiness, groundGap);
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

    private static Support scanSupport(ClientWorld world, double cx, double cz, double half, double feetYd, int feetCell, CollisionContext ctx) {
        double slipperiness = MovementBlocks.slipperiness(StateTypes.AIR);
        double maxTop = Double.NEGATIVE_INFINITY;
        double feetTop = Double.NEGATIVE_INFINITY;

        for (double[] point : footprint(cx, cz, half)) {
            int px = floor(point[0]), pz = floor(point[1]);
            CollisionShape feetShape = BlockShapes.shapeOf(world.getBlockState(px, feetCell, pz), feetCell, ctx);
            if (!feetShape.isEmpty()) {
                double top = Math.min(feetCell + feetShape.maxY(), feetYd);
                if (top > feetTop) feetTop = top;
            }
            for (int dy = 1; dy <= 2; dy++) {
                int cellY = feetCell - dy;
                WrappedBlockState below = world.getBlockState(px, cellY, pz);
                CollisionShape shape = BlockShapes.shapeOf(below, cellY, ctx);
                if (shape.isEmpty()) continue;
                double topAbs = cellY + shape.maxY();
                if (topAbs <= feetYd + SUPPORT_TOP_EPS && topAbs > maxTop) {
                    maxTop = topAbs;
                    slipperiness = MovementBlocks.slipperiness(below.getType());
                }
            }
        }
        return new Support(Math.max(maxTop, feetTop), slipperiness);
    }

    private static double[][] footprint(double cx, double cz, double half) {
        return new double[][]{{cx - half, cz - half}, {cx + half, cz - half}, {cx - half, cz + half}, {cx + half, cz + half}, {cx, cz}};
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private record Support(double top, double slipperiness) {
    }

    private record Below(double bounceFactor, double slipperiness, double groundGap) {
    }

    private record Stuck(boolean active, double horizontal, double vertical) {
    }
}
