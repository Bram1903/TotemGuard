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

        double slipMin = Double.MAX_VALUE;
        double slipMax = -Double.MAX_VALUE;
        double jumpMin = Double.MAX_VALUE;
        double jumpMax = -Double.MAX_VALUE;
        double speedMax = -Double.MAX_VALUE;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                long below = reader.facts(x, belowY, z);
                double slip = StateFacts.slipperiness(below);
                if (slip < slipMin) slipMin = slip;
                if (slip > slipMax) slipMax = slip;

                long feet = reader.facts(x, feetCellY, z);
                double jumpBelow = StateFacts.jumpFactor(below);
                double jumpFeet = StateFacts.jumpFactor(feet);
                double jump = Math.min(jumpBelow, jumpFeet);
                if (jump < jumpMin) jumpMin = jump;
                double jumpHigh = Math.max(jumpBelow, jumpFeet);
                if (jumpHigh > jumpMax) jumpMax = jumpHigh;

                double speed = Math.max(StateFacts.speedFactor(below), StateFacts.speedFactor(feet));
                if (speed > speedMax) speedMax = speed;
            }
        }
        report.supportSlipMin(slipMin);
        report.supportSlipMax(slipMax);
        report.supportSpeedFactor(speedMax);
        report.supportJumpMin(jumpMin);
        report.supportJumpMax(jumpMax);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
