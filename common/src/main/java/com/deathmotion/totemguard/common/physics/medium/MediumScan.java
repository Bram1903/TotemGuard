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

    public static final double NO_EYE_SAMPLE = Double.NaN;

    private static final double FLUID_BOX_DEFLATE = 0.001;
    private static final double LEGACY_EYE_FLUID_OFFSET = 0.11111111111111111;

    private MediumScan() {
    }

    public static void sample(BlockReader reader, MediumSample out,
                              boolean pushedByFluid, boolean normalizePush,
                              boolean lavaFast, boolean modernFluidPush,
                              boolean weavingCobweb, boolean stuckApplies, boolean stuckOnPath,
                              double eyeY,
                              double minX, double feetY, double minZ,
                              double maxX, double headY, double maxZ,
                              double sweptMinX, double sweptMinY, double sweptMinZ,
                              double sweptMaxX, double sweptMaxY, double sweptMaxZ) {
        out.reset();
        out.boxMinX(minX);
        out.boxFeetY(feetY);
        out.boxMinZ(minZ);
        out.boxMaxX(maxX);
        out.boxHeadY(headY);
        out.boxMaxZ(maxZ);
        out.eyeSampleY(eyeY);
        if (pushedByFluid) {
            FlowSolver.solve(reader, out, lavaFast, normalizePush, modernFluidPush,
                    minX, feetY, minZ, maxX, headY, maxZ);
        }
        int sx0 = floor(minX), sx1 = floor(maxX);
        int sy0 = floor(feetY), sy1 = floor(headY);
        int sz0 = floor(minZ), sz1 = floor(maxZ);
        int fx0 = floor(minX + FLUID_BOX_DEFLATE), fx1 = ceil(maxX - FLUID_BOX_DEFLATE) - 1;
        int fy0 = floor(feetY + FLUID_BOX_DEFLATE), fy1 = ceil(headY - FLUID_BOX_DEFLATE) - 1;
        int fz0 = floor(minZ + FLUID_BOX_DEFLATE), fz1 = ceil(maxZ - FLUID_BOX_DEFLATE) - 1;
        out.fluidCellX0(fx0);
        out.fluidCellX1(fx1);
        out.fluidCellY0(fy0);
        out.fluidCellY1(fy1);
        out.fluidCellZ0(fz0);
        out.fluidCellZ1(fz1);
        int wx0 = floor(sweptMinX), wx1 = floor(sweptMaxX);
        int wy0 = floor(sweptMinY), wy1 = floor(sweptMaxY);
        int wz0 = floor(sweptMinZ), wz1 = floor(sweptMaxZ);
        double fluidThreshold = feetY + FLUID_BOX_DEFLATE;
        double bestStuckHorizontal = -1.0;
        boolean sweptStuck = false;
        int feetBlockX = floor((minX + maxX) / 2.0);
        int feetBlockZ = floor((minZ + maxZ) / 2.0);

        for (int x = wx0; x <= wx1; x++) {
            for (int z = wz0; z <= wz1; z++) {
                boolean inStartColumn = x >= sx0 && x <= sx1 && z >= sz0 && z <= sz1;
                boolean inFluidColumn = x >= fx0 && x <= fx1 && z >= fz0 && z <= fz1;
                long factsAbove = 0L;
                boolean aboveValid = false;
                for (int y = wy1; y >= wy0; y--) {
                    long facts = reader.facts(x, y, z);
                    boolean inStart = inStartColumn && y >= sy0 && y <= sy1;
                    boolean inFluidBox = inFluidColumn && y >= fy0 && y <= fy1;
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
                        if (inFluidBox && !(lava ? out.lava() : out.water())) {
                            double height = StateFacts.is(above, lava ? StateFacts.LAVA : StateFacts.WATER)
                                    ? 1.0
                                    : StateFacts.fluidHeight(facts);
                            if (y + height >= fluidThreshold) {
                                if (lava) {
                                    out.lava(true);
                                } else {
                                    out.water(true);
                                    out.wetCellFound(true);
                                    out.wetCellX(x);
                                    out.wetCellY(y);
                                    out.wetCellZ(z);
                                    out.wetCellSurface(y + height);
                                }
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
        out.waterAtFeet(StateFacts.is(
                reader.facts(feetBlockX, feetBlockY, feetBlockZ), StateFacts.WATER));
        out.eyeInWater(eyeInWater(reader, modernFluidPush, eyeY, feetBlockX, feetBlockZ));
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

    private static boolean eyeInWater(BlockReader reader, boolean modernFluidPush,
                                      double eyeY, int blockX, int blockZ) {
        if (Double.isNaN(eyeY)) return false;
        double sampleY = modernFluidPush ? eyeY : eyeY - LEGACY_EYE_FLUID_OFFSET;
        int blockY = floor(sampleY);
        long facts = reader.facts(blockX, blockY, blockZ);
        if (!StateFacts.is(facts, StateFacts.WATER)) return false;
        double height = StateFacts.is(reader.facts(blockX, blockY + 1, blockZ), StateFacts.WATER)
                ? 1.0
                : StateFacts.fluidHeight(facts);
        double surface = blockY + height;
        return modernFluidPush ? sampleY <= surface : sampleY < surface;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }
}
