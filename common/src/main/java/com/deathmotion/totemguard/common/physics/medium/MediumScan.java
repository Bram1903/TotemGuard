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
                              boolean pushedByFluid, boolean lavaFast, boolean modernFluidPush,
                              boolean weavingCobweb, boolean stuckApplies, boolean stuckOnPath,
                              double minX, double feetY, double minZ,
                              double maxX, double headY, double maxZ,
                              double sweptMinX, double sweptMinY, double sweptMinZ,
                              double sweptMaxX, double sweptMaxY, double sweptMaxZ) {
        out.reset();
        if (pushedByFluid) {
            FlowSolver.solve(reader, out, lavaFast, false, modernFluidPush,
                    minX, feetY, minZ, maxX, headY, maxZ);
        }
        int sx0 = floor(minX), sx1 = floor(maxX);
        int sy0 = floor(feetY), sy1 = floor(headY);
        int sz0 = floor(minZ), sz1 = floor(maxZ);
        int wx0 = floor(sweptMinX), wx1 = floor(sweptMaxX);
        int wy0 = floor(sweptMinY), wy1 = floor(sweptMaxY);
        int wz0 = floor(sweptMinZ), wz1 = floor(sweptMaxZ);
        double fluidThreshold = feetY - FLUID_SURFACE_MARGIN;
        double bestStuckHorizontal = -1.0;
        boolean sweptStuck = false;
        int feetBlockX = floor((minX + maxX) / 2.0);
        int feetBlockZ = floor((minZ + maxZ) / 2.0);

        for (int x = wx0; x <= wx1; x++) {
            for (int z = wz0; z <= wz1; z++) {
                boolean inStartColumn = x >= sx0 && x <= sx1 && z >= sz0 && z <= sz1;
                long factsAbove = 0L;
                boolean aboveValid = false;
                for (int y = wy1; y >= wy0; y--) {
                    long facts = reader.facts(x, y, z);
                    boolean inStart = inStartColumn && y >= sy0 && y <= sy1;
                    if (stuckOnPath && StateFacts.is(facts, StateFacts.STUCK)) {
                        sweptStuck = true;
                        boolean powderSnow = StateFacts.isPowderSnowStuck(facts);
                        if (powderSnow && inStartColumn) out.powderSnowSwept(true);
                        boolean arms = powderSnow
                                ? x == feetBlockX && z == feetBlockZ && y == sy0
                                : inStart;
                        if (stuckApplies && arms) {
                            double horizontal = StateFacts.stuckHorizontal(facts, weavingCobweb);
                            if (horizontal > bestStuckHorizontal) {
                                bestStuckHorizontal = horizontal;
                                out.stuck(true);
                                out.stuckHorizontal(horizontal);
                                out.stuckVertical(StateFacts.stuckVertical(facts, weavingCobweb));
                            }
                        }
                    }
                    if (inStart && StateFacts.is(facts, StateFacts.ANY_FLUID)) {
                        long above = aboveValid ? factsAbove : reader.facts(x, y + 1, z);
                        boolean lava = StateFacts.is(facts, StateFacts.LAVA);
                        if (!(lava ? out.lava() : out.water())) {
                            double height = StateFacts.is(above, StateFacts.ANY_FLUID)
                                    ? 1.0
                                    : StateFacts.fluidHeight(facts);
                            if (y + height >= fluidThreshold) {
                                if (lava) out.lava(true);
                                else out.water(true);
                            }
                        }
                        if (StateFacts.is(facts, StateFacts.BUBBLE_COLUMN)
                                && !StateFacts.is(facts, StateFacts.BUBBLE_DRAG)) {
                            boolean surface = !StateFacts.is(above, StateFacts.BUBBLE_COLUMN);
                            double cap = surface
                                    ? BlockTraits.BUBBLE_COLUMN_SURFACE_ASCENT
                                    : BlockTraits.BUBBLE_COLUMN_INSIDE_ASCENT;
                            if (cap > out.bubbleAscent()) out.bubbleAscent(cap);
                        }
                    }
                    factsAbove = facts;
                    aboveValid = true;
                }
            }
        }
        out.stuckAlongPath(sweptStuck);

        int feetBlockY = floor(feetY);
        out.swimSteerWater(StateFacts.is(
                reader.facts(feetBlockX, floor(feetY + 0.9), feetBlockZ), StateFacts.ANY_FLUID));
        int feetClientId = reader.clientStateId(feetBlockX, feetBlockY, feetBlockZ);
        if (StateFacts.is(reader.factsForClientId(feetClientId), StateFacts.CLIMBABLE)) {
            out.climbable(true);
            out.climbableUncertain(reader.uncertain(feetBlockX, feetBlockY, feetBlockZ));
        } else {
            WrappedBlockState feetState = reader.stateForClientId(feetClientId);
            WrappedBlockState belowState = reader.stateForClientId(
                    reader.clientStateId(feetBlockX, feetBlockY - 1, feetBlockZ));
            if (BlockTraits.trapdoorUsableAsLadder(feetState, belowState)) {
                out.climbable(true);
                out.climbableUncertain(reader.uncertain(feetBlockX, feetBlockY, feetBlockZ)
                        || reader.uncertain(feetBlockX, feetBlockY - 1, feetBlockZ));
            }
        }
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
