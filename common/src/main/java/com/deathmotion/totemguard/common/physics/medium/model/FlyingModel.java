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
import com.deathmotion.totemguard.common.physics.control.GhastControl;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSample;

public final class FlyingModel implements MediumModel {

    private static final double WATER_DRAG = 0.8f;
    private static final double LAVA_DRAG = 0.5;

    private double drag = LandModel.AIR_FRICTION;

    public void prepare(MediumSample sample) {
        drag = sample.water() ? WATER_DRAG : sample.lava() ? LAVA_DRAG : LandModel.AIR_FRICTION;
    }

    public double drag() {
        return drag;
    }

    @Override
    public MediumKind kind() {
        return MediumKind.FLYING;
    }

    @Override
    public double accelBound(ControlEnvelope input, GroundFacts ground) {
        return ((GhastControl) input).horizontalReach();
    }

    @Override
    public void verticalOptions(ControlEnvelope input, GroundFacts ground, ContactReport contact, AreaBounds bounds) {
        GhastControl control = (GhastControl) input;
        bounds.ceiling(bounds.ceiling() + control.up());
        bounds.floor(bounds.floor() - control.down());
    }

    @Override
    public double frictionMax(ControlEnvelope input, GroundFacts ground) {
        return drag;
    }

    @Override
    public double advanceVertical(double verticalVelocity, ControlEnvelope input) {
        return verticalVelocity * drag;
    }
}
