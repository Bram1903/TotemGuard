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
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.physics.simulation.SpawnQueue;
import com.deathmotion.totemguard.common.physics.simulation.TickState;
import com.deathmotion.totemguard.common.physics.trace.TraceFrame;

public final class AirRegimeSpawnRule {

    private AirRegimeSpawnRule() {
    }

    public static void queue(TickState state, boolean deltaZeroedLastTick, boolean claimedGround,
                             AreaBounds chosenBounds, SpawnQueue queue) {
        if ((!state.stepped && !deltaZeroedLastTick)
                || (!state.ground.groundedEnd() && !state.stepFromFall)) return;
        if (!state.landModel()) return;
        if (claimedGround && state.previousClaimedGround) return;
        if (!claimedGround && !state.input.jumpPossible()) return;
        double carryFriction = (state.previousClaimedGround ? state.frictionMax
                : LandModel.computeModifiedFriction(MotionDefaults.AIR_FRICTION, state.input.airDragModifier()))
                * state.speedFactor;
        double fallVy = state.medium.advanceVertical(0.0, state.input);
        double jumpVy = state.medium.advanceVertical(state.input.jumpTakeoff(), state.input);
        queue.queue(CarriedHypotheses.Kind.AIR_REGIME, TraceFrame.SPAWN_AIR_REGIME,
                new MotionArea(chosenBounds.legalX() * carryFriction, chosenBounds.legalZ() * carryFriction,
                        0.0, fallVy, Math.max(fallVy, jumpVy)));
    }
}
