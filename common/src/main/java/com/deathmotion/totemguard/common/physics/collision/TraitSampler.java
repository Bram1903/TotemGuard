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

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;

public final class TraitSampler {

    private static final double SAMPLE_DEPTH = 0.500001;

    private TraitSampler() {
    }

    public static void sample(BlockReader reader, ContactReport report,
                              double centerX, double feetY, double centerZ, double half) {
        int belowY = floor(feetY - SAMPLE_DEPTH);
        int feetCellY = floor(feetY);
        int x0 = floor(centerX - half), x1 = floor(centerX + half);
        int z0 = floor(centerZ - half), z1 = floor(centerZ + half);

        int[] belowIds = new int[BlockReader.MAX_REALITIES];
        int[] feetIds = new int[BlockReader.MAX_REALITIES];

        double slipMin = Double.MAX_VALUE;
        double slipMax = -Double.MAX_VALUE;
        double jumpMin = Double.MAX_VALUE;
        double jumpMax = -Double.MAX_VALUE;
        double speedMax = -Double.MAX_VALUE;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int belowCount = reader.realities(x, belowY, z, belowIds);
                int feetCount = reader.realities(x, feetCellY, z, feetIds);

                double slipLow = minSlip(reader, belowIds, belowCount);
                if (slipLow < slipMin) slipMin = slipLow;
                double slipHigh = maxSlip(reader, belowIds, belowCount);
                if (slipHigh > slipMax) slipMax = slipHigh;

                double jump = Math.min(minJump(reader, belowIds, belowCount), minJump(reader, feetIds, feetCount));
                if (jump < jumpMin) jumpMin = jump;
                double jumpHigh = Math.max(maxJump(reader, belowIds, belowCount), maxJump(reader, feetIds, feetCount));
                if (jumpHigh > jumpMax) jumpMax = jumpHigh;

                double speed = Math.max(maxSpeed(reader, belowIds, belowCount), maxSpeed(reader, feetIds, feetCount));
                if (speed > speedMax) speedMax = speed;
            }
        }
        report.supportSlipMin(slipMin);
        report.supportSlipMax(slipMax);
        report.supportSpeedFactor(speedMax);
        report.supportJumpMin(jumpMin);
        report.supportJumpMax(jumpMax);
    }

    private static double minSlip(BlockReader reader, int[] ids, int count) {
        double value = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            value = Math.min(value, StateFacts.slipperiness(reader.factsForClientId(ids[i])));
        }
        return value;
    }

    private static double maxSlip(BlockReader reader, int[] ids, int count) {
        double value = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            value = Math.max(value, StateFacts.slipperiness(reader.factsForClientId(ids[i])));
        }
        return value;
    }

    private static double minJump(BlockReader reader, int[] ids, int count) {
        double value = Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            value = Math.min(value, StateFacts.jumpFactor(reader.factsForClientId(ids[i])));
        }
        return value;
    }

    private static double maxJump(BlockReader reader, int[] ids, int count) {
        double value = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            value = Math.max(value, StateFacts.jumpFactor(reader.factsForClientId(ids[i])));
        }
        return value;
    }

    private static double maxSpeed(BlockReader reader, int[] ids, int count) {
        double value = -Double.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            value = Math.max(value, StateFacts.speedFactor(reader.factsForClientId(ids[i])));
        }
        return value;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
