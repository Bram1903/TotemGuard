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
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;

public final class KnockbackSetSpawnRule {

    private KnockbackSetSpawnRule() {
    }

    public static void spawn(ExternalVelocityData external, MediumModel medium, ControlEnvelope input,
                             CarriedHypotheses carried, SpawnQueue queue) {
        if (!external.isActive() || !external.hasSet()) return;
        double slack = external.slack();
        double rawLow = external.y() - slack;
        double rawHigh = external.y() + slack;
        double advancedLow = medium.advanceVertical(rawLow, input);
        double advancedHigh = medium.advanceVertical(rawHigh, input);
        double floor = Math.min(rawLow, Math.min(advancedLow, advancedHigh));
        double ceiling = Math.max(rawHigh, Math.max(advancedLow, advancedHigh));
        queue.spawnNow(carried, CarriedHypotheses.Kind.KNOCKBACK_SET, TraceFrame.SPAWN_KNOCKBACK,
                new MotionArea(external.x(), external.z(), slack, floor, ceiling));
    }
}
