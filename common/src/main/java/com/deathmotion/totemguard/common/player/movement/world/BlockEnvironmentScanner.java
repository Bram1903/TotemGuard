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

    public static BlockEnvironment scan(ClientWorld world, Location current, Location previous, double width, double poseHeight, boolean sneaking) {
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
        Below below = scanBelow(world, current, width, new CollisionContext(current.getY(), sneaking));
        return new BlockEnvironment(true, fluid[0], climbable, stuck.active, stuck.horizontal, stuck.vertical,
                below.bouncy, below.slipperiness, below.groundGap);
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

    private static Below scanBelow(ClientWorld world, Location feet, double width, CollisionContext ctx) {
        double half = width / 2.0;
        double feetYd = feet.getY();
        int y = floor(feetYd - SUPPORT_BLOCK_OFFSET);
        int feetCell = floor(feetYd);
        double cx = feet.getX(), cz = feet.getZ();

        boolean bouncy = false;
        boolean feetCellSolid = false;
        double slipperiness = MovementBlocks.slipperiness(StateTypes.AIR);
        double maxTop = Double.NEGATIVE_INFINITY;

        double[][] footprint = {{cx - half, cz - half}, {cx + half, cz - half}, {cx - half, cz + half}, {cx + half, cz + half}, {cx, cz}};
        for (double[] point : footprint) {
            int px = floor(point[0]), pz = floor(point[1]);
            StateType nearType = world.getBlockState(px, y, pz).getType();
            if (nearType != StateTypes.AIR && MovementBlocks.isBouncy(nearType)) bouncy = true;
            WrappedBlockState feetState = world.getBlockState(px, feetCell, pz);
            if (!BlockShapes.shapeOf(feetState, feetCell, ctx).isEmpty()) feetCellSolid = true;
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
        double groundGap = feetCellSolid ? 0.0
                : (maxTop == Double.NEGATIVE_INFINITY ? UNSUPPORTED_GAP : feetYd - maxTop);
        return new Below(bouncy, slipperiness, groundGap);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private record Below(boolean bouncy, double slipperiness, double groundGap) {
    }

    private record Stuck(boolean active, double horizontal, double vertical) {
    }
}
