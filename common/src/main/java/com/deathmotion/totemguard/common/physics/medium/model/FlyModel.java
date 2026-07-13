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

package com.deathmotion.totemguard.common.physics.medium.model;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;

public final class FlyModel implements MediumModel {

    private static final double VERTICAL_DAMPING = 0.6;

    @Override
    public MediumKind kind() {
        return MediumKind.FLYING;
    }

    @Override
    public double accelBound(ControlEnvelope input, GroundFacts ground) {
        return input.flyAccel();
    }

    @Override
    public double accelBoundBase(ControlEnvelope input, GroundFacts ground) {
        return input.flyAccelBase();
    }

    @Override
    public double accelBoundBaseMin(ControlEnvelope input, GroundFacts ground) {
        return input.flyAccelBase();
    }

    @Override
    public void verticalOptions(ControlEnvelope input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        bounds.ceiling(bounds.ceiling() + input.flyVertical());
        bounds.lowerFloor(bounds.floor() - input.flyVertical());
        if (input.jumpPossible()) bounds.raiseCeiling(input.jumpTakeoff() + input.flyVertical());
        if (ground.groundedStart() || ground.groundedEnd() || contact.startOverlapping()) {
            bounds.raiseCeiling(input.stepHeight());
        }
        bounds.enforceDescentFloor(true);
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        return LandModel.computeModifiedFriction(LandModel.AIR_FRICTION, input.airDragModifier());
    }

    @Override
    public double advanceVertical(double verticalVelocity, ControlEnvelope input) {
        return verticalVelocity * VERTICAL_DAMPING;
    }
}
