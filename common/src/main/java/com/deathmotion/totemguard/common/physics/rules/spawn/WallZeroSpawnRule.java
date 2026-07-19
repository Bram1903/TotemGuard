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

package com.deathmotion.totemguard.common.physics.rules.spawn;

import com.deathmotion.totemguard.common.physics.area.CarriedHypotheses;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.simulation.SpawnQueue;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;

public final class WallZeroSpawnRule {

    private WallZeroSpawnRule() {
    }

    public static void queue(ContactReport contact, boolean endTickGate, boolean claimedHorizontalCollision,
                             MotionArea advanced, SpawnQueue queue) {
        if (contact.stepUsedHeight() > 0.0) return;
        boolean penetration = contact.collidedX() || contact.collidedZ();
        if (!penetration && !contact.wallNear()) return;
        if (endTickGate && !claimedHorizontalCollision) return;
        double centerX = advanced.centerX();
        double centerZ = advanced.centerZ();
        if (penetration) {
            double zeroedX = contact.collidedX() ? 0.0 : centerX;
            double zeroedZ = contact.collidedZ() ? 0.0 : centerZ;
            if (zeroedX != centerX || zeroedZ != centerZ) {
                queue.queue(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_COLLIDE_ZERO,
                        new MotionArea(zeroedX, zeroedZ, advanced.slack(),
                                advanced.floorVy(), advanced.ceilVy()));
            }
            return;
        }
        if (Math.abs(centerX) > 1.0e-9) {
            queue.queue(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_COLLIDE_ZERO,
                    new MotionArea(0.0, centerZ, advanced.slack(),
                            advanced.floorVy(), advanced.ceilVy()));
        }
        if (Math.abs(centerZ) > 1.0e-9) {
            queue.queue(CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_COLLIDE_ZERO,
                    new MotionArea(centerX, 0.0, advanced.slack(),
                            advanced.floorVy(), advanced.ceilVy()));
        }
    }
}
