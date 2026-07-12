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

package com.deathmotion.totemguard.common.physics.verdict;

import com.deathmotion.totemguard.common.physics.body.BodyKind;
import com.deathmotion.totemguard.common.physics.ground.GroundState;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.util.ClientMath;

public record PhysicsVerdict(
        MotionStream stream,
        BodyKind body,
        TickOutcome outcome,
        DeclineReason declineReason,
        BoundBreach breach,
        double observedX, double observedY, double observedZ,
        double horizontalExcess, double ascentExcess, double descentExcess, double phaseExcess,
        double boundCenterX, double boundCenterZ, double boundRadius,
        double boundCeiling, double boundFloor,
        MediumKind medium,
        GroundState ground,
        boolean inventoryOpen,
        boolean knockbackConsumed,
        boolean improperSprint,
        MitigationOutcome mitigation,
        FallFinding fall) {

    public static final PhysicsVerdict INITIAL = new PhysicsVerdict(
            MotionStream.SELF, BodyKind.PLAYER,
            TickOutcome.SEEDED, null, null,
            0.0, 0.0, 0.0,
            0.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 0.0,
            0.0, 0.0,
            MediumKind.LAND, GroundState.AMBIGUOUS,
            false, false, false,
            MitigationOutcome.NONE, FallFinding.NONE);

    public boolean clean() {
        return breach == null;
    }

    public double observedSpeed() {
        return ClientMath.horizontalDistance(observedX, observedZ);
    }

    public PhysicsVerdict withOutcome(MitigationOutcome mitigation, FallFinding fall, boolean improperSprint) {
        return new PhysicsVerdict(stream, body, outcome, declineReason, breach,
                observedX, observedY, observedZ,
                horizontalExcess, ascentExcess, descentExcess, phaseExcess,
                boundCenterX, boundCenterZ, boundRadius,
                boundCeiling, boundFloor,
                medium, ground,
                inventoryOpen, knockbackConsumed, improperSprint,
                mitigation, fall);
    }

    public PhysicsVerdict withKnockbackBreach(double ignoredExcess) {
        return new PhysicsVerdict(stream, body, outcome, declineReason, BoundBreach.KNOCKBACK,
                observedX, observedY, observedZ,
                ignoredExcess, ascentExcess, descentExcess, phaseExcess,
                boundCenterX, boundCenterZ, boundRadius,
                boundCeiling, boundFloor,
                medium, ground,
                inventoryOpen, knockbackConsumed, improperSprint,
                mitigation, fall);
    }
}
