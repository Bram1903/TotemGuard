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

public final class EntitySupport {

    private static final double SUPPORT_HORIZONTAL_PAD = 0.1;
    private static final double SUPPORT_TOP_EPS = 0.03;
    private static final double SUPPORT_MAX_DROP = 2.0;

    private EntitySupport() {
    }

    public static double standableSupportTop(Collection<TrackedEntity> tracked, int standableCount,
                                             double pMinX, double pMinZ, double pMaxX, double pMaxZ, double feetY) {
        if (standableCount == 0) return Double.NEGATIVE_INFINITY;
        double best = Double.NEGATIVE_INFINITY;
        for (TrackedEntity entity : tracked) {
            if (!entity.isPositioned() || !entity.isStandable()) continue;

            double half = entity.halfWidth() + SUPPORT_HORIZONTAL_PAD;
            double eMinX = Math.min(entity.getPrevRenderX(), entity.getTargetX()) - half;
            double eMaxX = Math.max(entity.getPrevRenderX(), entity.getTargetX()) + half;
            double eMinZ = Math.min(entity.getPrevRenderZ(), entity.getTargetZ()) - half;
            double eMaxZ = Math.max(entity.getPrevRenderZ(), entity.getTargetZ()) + half;
            if (pMaxX <= eMinX || pMinX >= eMaxX) continue;
            if (pMaxZ <= eMinZ || pMinZ >= eMaxZ) continue;

            double top = Math.max(entity.getPrevRenderY(), entity.getTargetY()) + entity.height();
            if (top > feetY + SUPPORT_TOP_EPS) continue;
            if (top < feetY - SUPPORT_MAX_DROP) continue;
            if (top > best) best = top;
        }
        return best;
    }
}
