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

    private static final double VANILLA_PUSH_PER_TICK = 0.05;
    private static final double PUSH_CEILING = 1.5;

    private double carried;

    public void advance(EntityTracker entities, double friction,
                        double sweptMinX, double sweptMinY, double sweptMinZ,
                        double sweptMaxX, double sweptMaxY, double sweptMaxZ,
                        double playerHalfWidth, double playerHeight) {
        int count = entities.countPushableNear(sweptMinX, sweptMinY, sweptMinZ,
                sweptMaxX, sweptMaxY, sweptMaxZ, playerHalfWidth, playerHeight);
        double next = carried * friction + count * VANILLA_PUSH_PER_TICK;
        carried = Math.min(PUSH_CEILING, Math.max(0.0, next));
    }

    public double apply(AreaBounds bounds) {
        if (carried > 0.0) bounds.expandRadius(carried);
        return carried;
    }

    public double carried() {
        return carried;
    }

    public void reset() {
        carried = 0.0;
    }
}
