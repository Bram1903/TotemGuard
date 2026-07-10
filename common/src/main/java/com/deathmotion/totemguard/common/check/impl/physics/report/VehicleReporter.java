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

package com.deathmotion.totemguard.common.check.impl.physics.report;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.physics.vehicle.VehicleEngine;
import com.deathmotion.totemguard.common.physics.vehicle.VehicleVerdict;

import java.util.Map;

public final class VehicleReporter {

    private final Flagger flagger;
    private final VehicleEngine engine;
    private final TGPlatform platform;

    public VehicleReporter(Flagger flagger, VehicleEngine engine, TGPlatform platform) {
        this.flagger = flagger;
        this.engine = engine;
        this.platform = platform;
    }

    public void report(VehicleVerdict verdict) {
        if (!verdict.breach()) return;

        boolean flagged = flagger.flag(Map.of("tg_physics_type", verdict.label()),
                "{0} | observed={1} allowed={2} | over={3}",
                verdict.label(), PhysicsFormat.d(verdict.observed()),
                PhysicsFormat.d(verdict.bound()), PhysicsFormat.d(verdict.excess()));
        if (flagged && platform.getConfigRepository().configView().physicsEngineSetback()) {
            engine.requestSetback();
        }
    }
}
