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

package com.deathmotion.totemguard.common.physics.push;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;

public final class EntityPushTracker {

    private static final double PUSH_PER_ENTITY = 0.08;
    private static final double MAX_PUSH = 0.30;

    private EntityPushTracker() {
    }

    public static double apply(AreaBounds bounds, EntityTracker entities,
                               double sweptMinX, double sweptMinY, double sweptMinZ,
                               double sweptMaxX, double sweptMaxY, double sweptMaxZ,
                               double playerHalfWidth, double playerHeight) {
        int count = entities.countPushableNear(sweptMinX, sweptMinY, sweptMinZ,
                sweptMaxX, sweptMaxY, sweptMaxZ, playerHalfWidth, playerHeight);
        if (count <= 0) return 0.0;
        double widen = Math.min(MAX_PUSH, count * PUSH_PER_ENTITY);
        bounds.expandRadius(widen);
        return widen;
    }
}
