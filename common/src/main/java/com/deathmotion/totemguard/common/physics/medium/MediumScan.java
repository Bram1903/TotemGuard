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

package com.deathmotion.totemguard.common.physics.medium;

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.BlockTraits;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

public final class MediumScan {

    private static final double FLUID_SURFACE_MARGIN = 0.01;

    private MediumScan() {
    }

    public static void sample(BlockReader reader, MediumSample out,
                              boolean pushedByFluid, boolean lavaFast,
                              double minX, double feetY, double minZ,
                              double maxX, double headY, double maxZ,
                              double sweptMinX, double sweptMinY, double sweptMinZ,
                              double sweptMaxX, double sweptMaxY, double sweptMaxZ) {
        out.reset();
        if (pushedByFluid) {
            FlowSolver.solve(reader, out, lavaFast, minX, feetY, minZ, maxX, headY, maxZ);
        }
        int x0 = floor(minX), x1 = floor(maxX);
        int y0 = floor(feetY), y1 = floor(headY);
        int z0 = floor(minZ), z1 = floor(maxZ);
        double fluidThreshold = feetY - FLUID_SURFACE_MARGIN;
        double bestStuckHorizontal = -1.0;

        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    long facts = reader.facts(x, y, z);
                    if (StateFacts.is(facts, StateFacts.ANY_FLUID)) {
                        boolean lava = StateFacts.is(facts, StateFacts.LAVA);
                        if (!(lava ? out.lava() : out.water())) {
                            double height = StateFacts.is(reader.facts(x, y + 1, z), StateFacts.ANY_FLUID)
                                    ? 1.0
                                    : BlockTraits.fluidSurfaceHeight(reader.state(x, y, z));
                            if (y + height >= fluidThreshold) {
                                if (lava) out.lava(true);
                                else out.water(true);
                            }
                        }
                        if (StateFacts.is(facts, StateFacts.BUBBLE_COLUMN)) {
                            WrappedBlockState state = reader.state(x, y, z);
                            if (!state.isDrag()) {
                                boolean surface = !StateFacts.is(reader.facts(x, y + 1, z), StateFacts.BUBBLE_COLUMN);
                                double cap = surface
                                        ? BlockTraits.BUBBLE_COLUMN_SURFACE_ASCENT
                                        : BlockTraits.BUBBLE_COLUMN_INSIDE_ASCENT;
                                if (cap > out.bubbleAscent()) out.bubbleAscent(cap);
                            }
                        }
                    }
                    if (StateFacts.is(facts, StateFacts.STUCK)) {
                        double horizontal = StateFacts.stuckHorizontal(facts);
                        if (horizontal > bestStuckHorizontal) {
                            bestStuckHorizontal = horizontal;
                            out.stuck(true);
                            out.stuckHorizontal(horizontal);
                            out.stuckVertical(StateFacts.stuckVertical(facts));
                        }
                    }
                }
            }
        }

        int feetBlockX = floor((minX + maxX) / 2.0);
        int feetBlockY = floor(feetY);
        int feetBlockZ = floor((minZ + maxZ) / 2.0);
        out.swimSteerWater(StateFacts.is(
                reader.facts(feetBlockX, floor(feetY + 0.9), feetBlockZ), StateFacts.ANY_FLUID));
        long feetFacts = reader.facts(feetBlockX, feetBlockY, feetBlockZ);
        if (StateFacts.is(feetFacts, StateFacts.CLIMBABLE)) {
            out.climbable(true);
        } else {
            WrappedBlockState feetState = reader.state(feetBlockX, feetBlockY, feetBlockZ);
            if (BlockTraits.trapdoorUsableAsLadder(feetState, reader.state(feetBlockX, feetBlockY - 1, feetBlockZ))) {
                out.climbable(true);
            }
        }

        out.stuckAlongPath(out.stuck() || sweptHasStuck(reader,
                sweptMinX, sweptMinY, sweptMinZ, sweptMaxX, sweptMaxY, sweptMaxZ));
    }

    private static boolean sweptHasStuck(BlockReader reader,
                                         double minX, double minY, double minZ,
                                         double maxX, double maxY, double maxZ) {
        int x0 = floor(minX), x1 = floor(maxX);
        int y0 = floor(minY), y1 = floor(maxY);
        int z0 = floor(minZ), z1 = floor(maxZ);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (StateFacts.is(reader.facts(x, y, z), StateFacts.STUCK)) return true;
                }
            }
        }
        return false;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
