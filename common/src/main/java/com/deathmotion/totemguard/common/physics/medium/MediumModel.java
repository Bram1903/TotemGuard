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

package com.deathmotion.totemguard.common.physics.medium;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.util.ClientMath;

public interface MediumModel {

    double SPRINT_JUMP_BOOST = 0.2;

    MediumKind kind();

    double accelBound(ControlEnvelope input, GroundFacts ground);

    default double accelBoundBase(ControlEnvelope input, GroundFacts ground) {
        return accelBound(input, ground);
    }

    default double accelBoundBaseMin(ControlEnvelope input, GroundFacts ground) {
        return accelBoundBase(input, ground);
    }

    default void horizontalOptions(ControlEnvelope input, GroundFacts ground, AreaBounds bounds) {
        if (input.horizontalInput()) {
            if (input.claimedInputExact()) {
                double maxAccel = accelBoundBase(input, ground);
                double minAccel = accelBoundBaseMin(input, ground);
                double mid = (maxAccel + minAccel) * 0.5;
                bounds.centerX(bounds.centerX() + input.claimedWorldX() * mid);
                bounds.centerZ(bounds.centerZ() + input.claimedWorldZ() * mid);
                double magnitude = ClientMath.horizontalDistance(input.claimedWorldX(), input.claimedWorldZ());
                bounds.expandRadius((maxAccel - minAccel) * 0.5 * magnitude
                        + input.claimedSpread() * maxAccel);
            } else {
                bounds.expandRadius(accelBound(input, ground));
            }
        }
    }

    void verticalOptions(ControlEnvelope input, GroundFacts ground, ContactReport contact, AreaBounds bounds);

    double frictionMax(ControlEnvelope input, GroundFacts ground);

    double advanceVertical(double verticalVelocity, ControlEnvelope input);
}
