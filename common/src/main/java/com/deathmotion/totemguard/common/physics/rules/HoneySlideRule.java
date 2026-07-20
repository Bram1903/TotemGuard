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

package com.deathmotion.totemguard.common.physics.rules;

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.PendingBlocks;
import com.deathmotion.totemguard.common.world.block.PredictedBlocks;
import com.deathmotion.totemguard.common.world.block.StateFacts;

public final class HoneySlideRule {

    private static final double SLIDE_SPEED = 0.05;
    private static final double BLOCK_TOP = 0.9375;
    private static final double EDGE_EPS = 1.0E-7;
    private static final double EDGE_REACH = 0.4375;
    private static final double SLIDE_START = -0.08;

    private HoneySlideRule() {
    }

    public static boolean slidePossible(BlockReader reader, boolean modernBlockEffects,
                                        double obsDy, boolean groundedEnd, double gravity,
                                        double centerX, double feetY, double centerZ,
                                        double halfWidth, double height) {
        if (groundedEnd) return false;
        double startThreshold = modernBlockEffects
                ? SLIDE_START - MotionDefaults.GRAVITY + gravity
                : SLIDE_START;
        if (obsDy >= startThreshold) return false;

        int x0 = floor(centerX - halfWidth), x1 = floor(centerX + halfWidth);
        int z0 = floor(centerZ - halfWidth), z1 = floor(centerZ + halfWidth);
        int y0 = floor(feetY), y1 = floor(feetY + height);
        double reach = EDGE_REACH + halfWidth;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (!honeyInAnyReality(reader, x, y, z)) continue;
                    if (feetY > y + BLOCK_TOP - EDGE_EPS) continue;
                    double dx = Math.abs(x + 0.5 - centerX);
                    double dz = Math.abs(z + 0.5 - centerZ);
                    if (dx + EDGE_EPS > reach || dz + EDGE_EPS > reach) return true;
                }
            }
        }
        return false;
    }

    private static boolean honeyInAnyReality(BlockReader reader, int x, int y, int z) {
        if (StateFacts.is(reader.facts(x, y, z), StateFacts.HONEY)) return true;
        if (!reader.uncertain(x, y, z)) return false;
        int pendingId = reader.pendingStateId(x, y, z);
        if (pendingId != PendingBlocks.NONE
                && StateFacts.is(reader.factsForClientId(pendingId), StateFacts.HONEY)) {
            return true;
        }
        int predictedId = reader.predictedStateId(x, y, z);
        return predictedId != PredictedBlocks.NONE
                && StateFacts.is(reader.factsForClientId(predictedId), StateFacts.HONEY);
    }

    public static double carriedVy(boolean modernBlockEffects, double gravity) {
        double applied = modernBlockEffects ? MotionDefaults.GRAVITY : gravity;
        return (-SLIDE_SPEED - applied) * MotionDefaults.VERTICAL_DRAG;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
