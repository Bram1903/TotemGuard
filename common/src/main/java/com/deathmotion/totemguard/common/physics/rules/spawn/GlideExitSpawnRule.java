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

import com.deathmotion.totemguard.common.physics.MotionDefaults;
import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.area.CarriedHypotheses;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.simulation.SpawnQueue;
import com.deathmotion.totemguard.common.physics.simulation.TickState;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;
import com.deathmotion.totemguard.common.player.data.GlideData;

public final class GlideExitSpawnRule {

    private GlideExitSpawnRule() {
    }

    public static void queue(TickState state, GlideData glide, AreaBounds chosenBounds,
                             MotionArea advanced, SpawnQueue queue) {
        if (!state.landModel() || !glide.justExited()) return;
        double airDecay = MotionDefaults.AIR_FRICTION * state.speedFactor;
        if (airDecay <= state.frictionMax * state.speedFactor) return;
        queue.queue(CarriedHypotheses.Kind.GLIDE_EXIT, TraceFrame.SPAWN_GLIDE_EXIT,
                new MotionArea(chosenBounds.legalX() * airDecay, chosenBounds.legalZ() * airDecay,
                        0.0, advanced.floorVy(), advanced.ceilVy()));
    }
}
