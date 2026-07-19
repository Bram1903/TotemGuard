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

package com.deathmotion.totemguard.common.physics.simulation;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.area.OutwardResidual;
import com.deathmotion.totemguard.common.util.ClientMath;

public final class TurnMomentumTracker {

    private static final double DRIFT_DECAY = 0.85;
    private static final double DRIFT_TOLERANCE = 1.0E-5;

    private final AreaBounds scratch = new AreaBounds();
    private double momentumX, momentumZ;
    private double drift;
    private boolean valid;

    public double excess(TickState state, boolean tainted) {
        if (tainted || state.medium == null || state.input == null || state.ground == null) {
            valid = false;
            return 0.0;
        }
        double friction = state.medium.frictionMax(state.input, state.ground);
        double speedFactor = state.speedFactor;
        if (!valid) {
            momentumX = state.dx * speedFactor * friction;
            momentumZ = state.dz * speedFactor * friction;
            drift = 0.0;
            valid = true;
            return 0.0;
        }
        scratch.reset(new MotionArea(momentumX, momentumZ, 0.0, 0.0, 0.0));
        state.medium.horizontalOptions(state.input, state.ground, scratch);
        double predX = scratch.centerX();
        double predZ = scratch.centerZ();
        double excess = OutwardResidual.excess(state.dx, state.dz, predX, predZ, scratch.radius());
        boolean inside = excess <= 0.0;

        double dirLen = ClientMath.horizontalDistance(predX, predZ);
        if (dirLen > 1.0e-6) {
            double perp = (predX * (state.dz - predZ) - predZ * (state.dx - predX)) / dirLen;
            drift = drift * DRIFT_DECAY + perp;
        } else {
            drift *= DRIFT_DECAY;
        }
        double driftExcess = Math.max(0.0, Math.abs(drift) - DRIFT_TOLERANCE);

        momentumX = (inside ? state.dx : predX) * speedFactor * friction;
        momentumZ = (inside ? state.dz : predZ) * speedFactor * friction;
        return Math.max(Math.max(0.0, excess), driftExcess);
    }

    public void invalidate() {
        valid = false;
    }
}
