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
import com.deathmotion.totemguard.common.physics.PhysicsEngine;
import com.deathmotion.totemguard.common.physics.body.BodyKind;
import com.deathmotion.totemguard.common.physics.verdict.BoundBreach;
import com.deathmotion.totemguard.common.physics.verdict.PhysicsVerdict;
import com.deathmotion.totemguard.common.util.ClientMath;

import java.util.Map;

public final class VehicleReporter {

    private final Flagger flagger;
    private final PhysicsEngine physics;
    private final TGPlatform platform;

    public VehicleReporter(Flagger flagger, PhysicsEngine physics, TGPlatform platform) {
        this.flagger = flagger;
        this.physics = physics;
        this.platform = platform;
    }

    public void report(PhysicsVerdict verdict) {
        BoundBreach breach = verdict.breach();
        if (breach == null) return;

        String label = label(verdict.body(), breach);
        double observed;
        double bound;
        double excess;
        switch (breach) {
            case DESCENT_FLOOR -> {
                observed = verdict.observedY();
                bound = verdict.boundFloor();
                excess = verdict.descentExcess();
            }
            case ASCENT -> {
                observed = verdict.observedY();
                bound = verdict.boundCeiling();
                excess = verdict.ascentExcess();
            }
            default -> {
                observed = verdict.observedSpeed();
                bound = ClientMath.horizontalDistance(verdict.boundCenterX(), verdict.boundCenterZ())
                        + verdict.boundRadius();
                excess = verdict.horizontalExcess();
            }
        }

        boolean flagged = flagger.flag(Map.of("tg_physics_type", label),
                "{0} | observed={1} allowed={2} | over={3}",
                label, PhysicsFormat.d(observed), PhysicsFormat.d(bound), PhysicsFormat.d(excess));
        if (flagged && platform.getConfigRepository().configView().physicsEngineSetback()) {
            physics.requestVehicleSetback();
        }
    }

    private static String label(BodyKind body, BoundBreach breach) {
        String kind = switch (body) {
            case BOAT -> "boat";
            case GHAST -> "ghast";
            case CAMEL -> "camel";
            case PIG -> "pig";
            case STRIDER -> "strider";
            default -> "horse";
        };
        String suffix = switch (breach) {
            case DESCENT_FLOOR -> "fly";
            case ASCENT -> "ascend";
            default -> "speed";
        };
        return kind + "-" + suffix;
    }
}
