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
import com.deathmotion.totemguard.common.world.block.Climbables;
import com.deathmotion.totemguard.common.world.block.FluidBlocks;
import com.deathmotion.totemguard.common.world.block.StuckBlocks;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;

import static com.deathmotion.totemguard.common.world.scan.Scans.floor;

final class MediumScanner {

    private static final double FLUID_SURFACE_MARGIN = 0.01;

    private MediumScanner() {
    }

    record Stuck(boolean active, double horizontal, double vertical) {
    }

    static boolean fluidReachesFeet(ClientWorld world, BoundingBox body, double feetY) {
        int x0 = floor(body.minX()), x1 = floor(body.maxX());
        int y0 = floor(body.minY()), y1 = floor(body.maxY());
        int z0 = floor(body.minZ()), z1 = floor(body.maxZ());
        double threshold = feetY - FLUID_SURFACE_MARGIN;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    WrappedBlockState state = world.getBlockState(x, y, z);
                    if (!FluidBlocks.isFluid(state)) continue;
                    double height = FluidBlocks.isFluid(world.getBlockState(x, y + 1, z))
                            ? 1.0
                            : FluidBlocks.surfaceHeight(state);
                    if (y + height >= threshold) return true;
                }
            }
        }
        return false;
    }

    static double bubbleColumnAscent(ClientWorld world, BoundingBox body) {
        int x0 = floor(body.minX()), x1 = floor(body.maxX());
        int y0 = floor(body.minY()), y1 = floor(body.maxY());
        int z0 = floor(body.minZ()), z1 = floor(body.maxZ());
        double cap = 0.0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    WrappedBlockState state = world.getBlockState(x, y, z);
                    if (!FluidBlocks.isBubbleColumn(state.getType()) || state.isDrag()) continue;
                    boolean surface = !FluidBlocks.isBubbleColumn(world.getBlockState(x, y + 1, z).getType());
                    cap = Math.max(cap, surface ? FluidBlocks.BUBBLE_COLUMN_SURFACE_ASCENT : FluidBlocks.BUBBLE_COLUMN_INSIDE_ASCENT);
                }
            }
        }
        return cap;
    }

    static Stuck stuck(ClientWorld world, Location feet, double width, double poseHeight) {
        BoundingBox body = BoundingBox.player(feet, width, poseHeight);
        double[] best = {-1.0, 1.0, 1.0};
        world.forEachBlock(body, state -> {
            StateType type = state.getType();
            if (StuckBlocks.isStuck(type)) {
                double h = StuckBlocks.horizontal(type);
                if (h > best[0]) {
                    best[0] = h;
                    best[1] = h;
                    best[2] = StuckBlocks.vertical(type);
                }
            }
            return false;
        });
        return new Stuck(best[0] >= 0.0, best[1], best[2]);
    }

    static boolean climbableAt(ClientWorld world, Location feet) {
        int x = floor(feet.getX()), y = floor(feet.getY()), z = floor(feet.getZ());
        WrappedBlockState state = world.getBlockState(x, y, z);
        if (Climbables.isClimbable(state.getType())) return true;
        return Climbables.trapdoorUsableAsLadder(state, world.getBlockState(x, y - 1, z));
    }
}
