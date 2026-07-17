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

import com.deathmotion.totemguard.common.physics.collision.ColliderBuffer;
import com.deathmotion.totemguard.common.physics.medium.model.BoatFloatModel;
import com.deathmotion.totemguard.common.world.block.BlockReader;

public final class BoatSnapRule {

    private BoatSnapRule() {
    }

    public static boolean eligible(BoatFloatModel model) {
        return model.snapEligible();
    }

    public static double snapDy(BoatFloatModel model, BlockReader reader,
                                double minX, double minZ, double maxX, double maxY, double maxZ,
                                double startY, double lastDy) {
        double surface = model.waterLevelAbove(reader, minX, minZ, maxX, maxY, maxZ, Math.min(0.0, lastDy));
        return surface - BoatFloatModel.BOAT_HEIGHT + BoatFloatModel.SNAP_RISE - startY;
    }

    public static boolean collisionFree(ColliderBuffer colliders,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        int count = colliders.count();
        for (int i = 0; i < count; i++) {
            if (colliders.maxX(i) > minX && colliders.minX(i) < maxX
                    && colliders.maxY(i) > minY && colliders.minY(i) < maxY
                    && colliders.maxZ(i) > minZ && colliders.minZ(i) < maxZ) {
                return false;
            }
        }
        return true;
    }
}
