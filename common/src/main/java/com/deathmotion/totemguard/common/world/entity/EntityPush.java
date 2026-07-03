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

package com.deathmotion.totemguard.common.world.entity;

import java.util.Collection;

public final class EntityPush {

    private EntityPush() {
    }

    public static int countPushableNear(Collection<TrackedEntity> tracked,
                                        double pMinX, double pMinY, double pMinZ,
                                        double pMaxX, double pMaxY, double pMaxZ,
                                        double playerHalfWidth, double playerHeight) {
        int count = 0;
        for (TrackedEntity entity : tracked) {
            if (!entity.isPositioned()) continue;
            if (!entity.isPushable()) continue;

            double eMinX = Math.min(entity.getPrevRenderX(), entity.getTargetX());
            double eMaxX = Math.max(entity.getPrevRenderX(), entity.getTargetX());
            double eMinY = Math.min(entity.getPrevRenderY(), entity.getTargetY());
            double eMaxY = Math.max(entity.getPrevRenderY(), entity.getTargetY());
            double eMinZ = Math.min(entity.getPrevRenderZ(), entity.getTargetZ());
            double eMaxZ = Math.max(entity.getPrevRenderZ(), entity.getTargetZ());

            double horizontalReach = playerHalfWidth + entity.halfWidth();
            boolean xOk = intervalGap(pMinX, pMaxX, eMinX, eMaxX) < horizontalReach;
            boolean zOk = intervalGap(pMinZ, pMaxZ, eMinZ, eMaxZ) < horizontalReach;
            boolean yOk = pMinY < eMaxY + entity.height() && eMinY < pMaxY + playerHeight;

            if (xOk && zOk && yOk) count++;
        }
        return count;
    }

    private static double intervalGap(double aMin, double aMax, double bMin, double bMax) {
        if (aMax < bMin) return bMin - aMax;
        if (bMax < aMin) return aMin - bMax;
        return 0.0;
    }
}
