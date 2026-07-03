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
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.world.block.BlockSpeedFactor;
import com.deathmotion.totemguard.common.world.block.BounceBlocks;
import com.deathmotion.totemguard.common.world.block.Slipperiness;
import com.deathmotion.totemguard.common.world.collisions.BlockShapes;
import com.deathmotion.totemguard.common.world.collisions.CollisionContext;
import com.deathmotion.totemguard.common.world.collisions.CollisionShape;
import com.deathmotion.totemguard.common.world.entity.EntitySupport;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import static com.deathmotion.totemguard.common.world.scan.Scans.floor;

final class SupportScanner {

    private static final double SUPPORT_BLOCK_OFFSET = 0.5000001;
    private static final double UNSUPPORTED_GAP = 10.0;

    private SupportScanner() {
    }

    record Support(double bounceFactor, double slipperinessMin, double slipperinessMax, double blockSpeedFactor, double groundGap) {
    }

    static Support scan(ClientWorld world, WorldEntityData entities, Location feet, Location previous, double width, CollisionContext ctx) {
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
            slipperinessMin = Slipperiness.of(StateTypes.AIR);
            slipperinessMax = slipperinessMin;
            bounceFactor = 0.0;
            blockSpeedFactor = 1.0;
        }

        double groundGap = supportTop == Double.NEGATIVE_INFINITY ? UNSUPPORTED_GAP : feetYd - supportTop;
        return new Support(bounceFactor, slipperinessMin, slipperinessMax, blockSpeedFactor, groundGap);
    }

    private static double feetBlockSpeedFactor(ClientWorld world, Location feet, double half) {
        double factor = Double.NEGATIVE_INFINITY;
        int fy = floor(feet.getY());
        int by = floor(feet.getY() - SUPPORT_BLOCK_OFFSET);
        for (double[] point : footprint(feet.getX(), feet.getZ(), half)) {
            int px = floor(point[0]), pz = floor(point[1]);
            double at = BlockSpeedFactor.of(world.getBlockState(px, fy, pz).getType());
            double below = at != 1.0 ? at : BlockSpeedFactor.of(world.getBlockState(px, by, pz).getType());
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
        return EntitySupport.standableSupportTop(entities.tracked(), entities.getStandableCount(), minX, minZ, maxX, maxZ, feetYd);
    }

    private static double scanBounce(ClientWorld world, double cx, double cz, double half, double feetYd) {
        int y = floor(feetYd - SUPPORT_BLOCK_OFFSET);
        double bounceFactor = 0.0;
        for (double[] point : footprint(cx, cz, half)) {
            StateType nearType = world.getBlockState(floor(point[0]), y, floor(point[1])).getType();
            bounceFactor = Math.max(bounceFactor, BounceBlocks.factor(nearType));
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
                    CollisionShape shape = BlockShapes.shapeOf(state, px, cellY, pz, ctx);
                    if (shape.isEmpty()) continue;
                    double cap = feetYd - cellY + Scans.SUPPORT_TOP_EPS;
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
            double slip = Slipperiness.of(world.getBlockState(floor(point[0]), y, floor(point[1])).getType());
            slipMin = Math.min(slipMin, slip);
            slipMax = Math.max(slipMax, slip);
        }
        return new double[]{slipMin, slipMax};
    }

    private static double[][] footprint(double cx, double cz, double half) {
        return new double[][]{{cx - half, cz - half}, {cx + half, cz - half}, {cx - half, cz + half}, {cx + half, cz + half}, {cx, cz}};
    }
}
