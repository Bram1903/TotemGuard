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

// Vanilla takes the FIRST improving step candidate, not the best one.
final class StepUpResolver {

    private static final int MAX_CANDIDATES = 16;

    private final double[] candidates = new double[MAX_CANDIDATES];

    double tryStep(ColliderBuffer buffer,
                   double baseMinX, double baseMinY, double baseMinZ,
                   double baseMaxX, double baseMaxY, double baseMaxZ,
                   double dx, double dz, double stepHeight, double skipHeight,
                   double flatDistSq, double[] result) {
        int candidateCount = collectCandidates(buffer, baseMinY, stepHeight, skipHeight);
        for (int c = 0; c < candidateCount; c++) {
            double rise = candidates[c];
            double clippedY = AxisClip.clip(buffer, AxisClip.AXIS_Y,
                    baseMinX, baseMinY, baseMinZ, baseMaxX, baseMaxY, baseMaxZ, rise, true);
            double y0 = baseMinY + clippedY;
            double y1 = baseMaxY + clippedY;
            double clippedX;
            double clippedZ;
            if (Math.abs(dx) < Math.abs(dz)) {
                clippedZ = AxisClip.clip(buffer, AxisClip.AXIS_Z,
                        baseMinX, y0, baseMinZ, baseMaxX, y1, baseMaxZ, dz, true);
                clippedX = AxisClip.clip(buffer, AxisClip.AXIS_X,
                        baseMinX, y0, baseMinZ + clippedZ, baseMaxX, y1, baseMaxZ + clippedZ, dx, true);
            } else {
                clippedX = AxisClip.clip(buffer, AxisClip.AXIS_X,
                        baseMinX, y0, baseMinZ, baseMaxX, y1, baseMaxZ, dx, true);
                clippedZ = AxisClip.clip(buffer, AxisClip.AXIS_Z,
                        baseMinX + clippedX, y0, baseMinZ, baseMaxX + clippedX, y1, baseMaxZ, dz, true);
            }
            double distSq = clippedX * clippedX + clippedZ * clippedZ;
            if (distSq > flatDistSq) {
                result[0] = clippedX;
                result[1] = clippedY;
                result[2] = clippedZ;
                return rise;
            }
        }
        return 0.0;
    }

    private int collectCandidates(ColliderBuffer buffer, double baseMinY, double stepHeight, double skipHeight) {
        int n = 0;
        int count = buffer.count();
        for (int i = 0; i < count && n < MAX_CANDIDATES; i++) {
            if (!ColliderBuffer.clipEligible(buffer.tagOf(i))) continue;
            n = addCandidate(buffer.minY(i) - baseMinY, stepHeight, skipHeight, n);
            if (n < MAX_CANDIDATES) {
                n = addCandidate(buffer.maxY(i) - baseMinY, stepHeight, skipHeight, n);
            }
        }
        java.util.Arrays.sort(candidates, 0, n);
        return n;
    }

    private int addCandidate(double height, double stepHeight, double skipHeight, int n) {
        if (height < 0.0 || height > stepHeight) return n;
        if (Math.abs(height - skipHeight) < 1.0e-9) return n;
        for (int i = 0; i < n; i++) {
            if (Math.abs(candidates[i] - height) < 1.0e-9) return n;
        }
        candidates[n] = height;
        return n + 1;
    }
}
