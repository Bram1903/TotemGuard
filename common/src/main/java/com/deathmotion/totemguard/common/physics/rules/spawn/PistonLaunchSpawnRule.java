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
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.simulation.SpawnQueue;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.player.data.PistonData;

public final class PistonLaunchSpawnRule {

    private PistonLaunchSpawnRule() {
    }

    public static void spawn(int launchMask, MediumModel medium, ControlEnvelope input,
                             CarriedHypotheses carried, SpawnQueue queue) {
        if (launchMask == 0) return;
        MotionArea union = carried.union();
        if ((launchMask & PistonData.LAUNCH_NEG_X) != 0) {
            spawnHorizontal(union, -PistonData.SLIME_LAUNCH, 0.0, carried, queue);
        }
        if ((launchMask & PistonData.LAUNCH_POS_X) != 0) {
            spawnHorizontal(union, PistonData.SLIME_LAUNCH, 0.0, carried, queue);
        }
        if ((launchMask & PistonData.LAUNCH_NEG_Z) != 0) {
            spawnHorizontal(union, 0.0, -PistonData.SLIME_LAUNCH, carried, queue);
        }
        if ((launchMask & PistonData.LAUNCH_POS_Z) != 0) {
            spawnHorizontal(union, 0.0, PistonData.SLIME_LAUNCH, carried, queue);
        }
        boolean up = (launchMask & PistonData.LAUNCH_POS_Y) != 0;
        boolean down = (launchMask & PistonData.LAUNCH_NEG_Y) != 0;
        if (up || down) {
            double rawHi = up ? PistonData.SLIME_LAUNCH : -PistonData.SLIME_LAUNCH;
            double rawLo = down ? -PistonData.SLIME_LAUNCH : PistonData.SLIME_LAUNCH;
            double advancedLo = medium.advanceVertical(rawLo, input);
            double advancedHi = medium.advanceVertical(rawHi, input);
            double floor = Math.min(rawLo, Math.min(advancedLo, advancedHi));
            double ceiling = Math.max(rawHi, Math.max(advancedLo, advancedHi));
            queue.spawnNow(carried, CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_PISTON_LAUNCH,
                    new MotionArea(union.centerX(), union.centerZ(), union.slack(), floor, ceiling));
        }
    }

    private static void spawnHorizontal(MotionArea union, double launchX, double launchZ,
                                        CarriedHypotheses carried, SpawnQueue queue) {
        queue.spawnNow(carried, CarriedHypotheses.Kind.SPARE, TraceFrame.SPAWN_PISTON_LAUNCH,
                new MotionArea(launchX != 0.0 ? launchX : union.centerX(),
                        launchZ != 0.0 ? launchZ : union.centerZ(),
                        union.slack(), union.floorVy(), union.ceilVy()));
    }
}
