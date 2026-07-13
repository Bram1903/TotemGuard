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

package com.deathmotion.totemguard.common.physics.body;

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.collision.ColliderBuffer;
import com.deathmotion.totemguard.common.player.data.Data;

public final class PoseTracker {

    private static final double POSE_FIT_EPS = 1.0e-7;

    private double lastPoseHeight = MotionDefaults.STANDING_HEIGHT;
    private double lastPoseBase = MotionDefaults.STANDING_HEIGHT;
    private double lastFeetClearance = Double.MAX_VALUE;

    private static double headroom(ColliderBuffer colliders,
                                   double minX, double feetY, double minZ, double maxX, double maxZ) {
        double headroom = Double.MAX_VALUE;
        int count = colliders.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(colliders.tagOf(i))) continue;
            if (!overlaps(minX, maxX, colliders.minX(i), colliders.maxX(i))) continue;
            if (!overlaps(minZ, maxZ, colliders.minZ(i), colliders.maxZ(i))) continue;
            if (colliders.maxY(i) <= feetY + POSE_FIT_EPS) continue;
            double bottom = colliders.minY(i);
            if (bottom <= feetY + POSE_FIT_EPS) return -1.0;
            double room = bottom - feetY;
            if (room < headroom) headroom = room;
        }
        return headroom;
    }

    private static boolean overlaps(double movingMin, double movingMax, double boxMin, double boxMax) {
        return movingMin + POSE_FIT_EPS < boxMax && movingMax - POSE_FIT_EPS > boxMin;
    }

    public double height(Data data) {
        if (data.isSleeping()) {
            lastPoseBase = MotionDefaults.SLEEPING_SIZE;
            lastPoseHeight = MotionDefaults.SLEEPING_SIZE;
            return lastPoseHeight;
        }
        double scale = data.getAttributeData().scale();
        double base;
        if (data.isSwimming() || data.isGliding() || data.isSpinAttacking()) {
            base = MotionDefaults.COMPACT_HEIGHT;
        } else {
            double want = data.isSneaking() ? MotionDefaults.SNEAKING_HEIGHT : MotionDefaults.STANDING_HEIGHT;
            if (fits(want * scale)) base = want;
            else if (fits(MotionDefaults.SNEAKING_HEIGHT * scale)) base = MotionDefaults.SNEAKING_HEIGHT;
            else if (fits(MotionDefaults.COMPACT_HEIGHT * scale)) base = MotionDefaults.COMPACT_HEIGHT;
            else base = lastPoseBase;
        }
        lastPoseBase = base;
        lastPoseHeight = base * scale;
        return lastPoseHeight;
    }

    public double lastHeight() {
        return lastPoseHeight;
    }

    public void updateHeadroom(ColliderBuffer colliders,
                               double minX, double feetY, double minZ, double maxX, double maxZ) {
        lastFeetClearance = headroom(colliders, minX, feetY, minZ, maxX, maxZ);
    }

    public void clearHistory() {
        lastFeetClearance = Double.MAX_VALUE;
        lastPoseBase = MotionDefaults.STANDING_HEIGHT;
    }

    private boolean fits(double height) {
        return lastFeetClearance >= height - POSE_FIT_EPS;
    }
}
