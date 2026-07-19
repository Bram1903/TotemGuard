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

public final class StriderLavaModel implements MediumModel {

    public static final double FLOAT_DRAG = 0.5;
    public static final double FLOAT_RISE = 0.05;

    private final LandModel land;

    public StriderLavaModel(LandModel land) {
        this.land = land;
    }

    public static double floated(double advanced) {
        return advanced * FLOAT_DRAG + FLOAT_RISE;
    }

    @Override
    public MediumKind kind() {
        return MediumKind.LAVA;
    }

    @Override
    public double accelBound(ControlEnvelope input, GroundFacts ground) {
        return land.accelBound(input, ground);
    }

    @Override
    public void verticalOptions(ControlEnvelope input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        double floatedFloor = floated(bounds.floor());
        double floatedCeiling = floated(bounds.ceiling());
        bounds.floor(Math.min(bounds.floor(), floatedFloor));
        bounds.ceiling(Math.max(bounds.ceiling(), floatedCeiling));
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        return land.frictionMax(input, ground);
    }

    @Override
    public double advanceVertical(double verticalVelocity, ControlEnvelope input) {
        return land.advanceVertical(verticalVelocity, input);
    }
}
