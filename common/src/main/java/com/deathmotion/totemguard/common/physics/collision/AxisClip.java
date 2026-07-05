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

public final class AxisClip {

    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_Z = 2;

    static final double EPS = 1.0e-7;

    private AxisClip() {
    }

    public static double clip(ColliderBuffer buffer, int axis,
                              double minX, double minY, double minZ,
                              double maxX, double maxY, double maxZ,
                              double distance, boolean clipEligibleOnly) {
        if (Math.abs(distance) < EPS) return 0.0;
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            if (clipEligibleOnly && !ColliderBuffer.clipEligible(buffer.tagOf(i))) continue;
            distance = clipBox(buffer, i, axis, minX, minY, minZ, maxX, maxY, maxZ, distance);
            if (Math.abs(distance) < EPS) return 0.0;
        }
        return distance;
    }

    private static double clipBox(ColliderBuffer b, int i, int axis,
                                  double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ,
                                  double distance) {
        double movingMin, movingMax, boxMin, boxMax;
        switch (axis) {
            case AXIS_X -> {
                if (!overlaps(minY, maxY, b.minY(i), b.maxY(i)) || !overlaps(minZ, maxZ, b.minZ(i), b.maxZ(i))) {
                    return distance;
                }
                movingMin = minX;
                movingMax = maxX;
                boxMin = b.minX(i);
                boxMax = b.maxX(i);
            }
            case AXIS_Y -> {
                if (!overlaps(minX, maxX, b.minX(i), b.maxX(i)) || !overlaps(minZ, maxZ, b.minZ(i), b.maxZ(i))) {
                    return distance;
                }
                movingMin = minY;
                movingMax = maxY;
                boxMin = b.minY(i);
                boxMax = b.maxY(i);
            }
            default -> {
                if (!overlaps(minX, maxX, b.minX(i), b.maxX(i)) || !overlaps(minY, maxY, b.minY(i), b.maxY(i))) {
                    return distance;
                }
                movingMin = minZ;
                movingMax = maxZ;
                boxMin = b.minZ(i);
                boxMax = b.maxZ(i);
            }
        }
        if (distance > 0.0) {
            double gap = boxMin - movingMax;
            if (gap >= -EPS) distance = Math.min(distance, gap);
        } else {
            double gap = boxMax - movingMin;
            if (gap <= EPS) distance = Math.max(distance, gap);
        }
        return distance;
    }

    static boolean overlaps(double movingMin, double movingMax, double boxMin, double boxMax) {
        return movingMin + EPS < boxMax && movingMax - EPS > boxMin;
    }
}
