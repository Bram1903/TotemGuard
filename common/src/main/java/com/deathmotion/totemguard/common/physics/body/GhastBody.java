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

package com.deathmotion.totemguard.common.physics.body;

import com.deathmotion.totemguard.common.physics.medium.model.FlyingModel;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

public final class GhastBody implements SimulationBody {

    private final FlyingModel flying = new FlyingModel();

    private TrackedEntity ridden;

    public void mount(TrackedEntity ridden, EntityType type) {
        this.ridden = ridden;
    }

    @Override
    public BodyKind kind() {
        return BodyKind.GHAST;
    }

    @Override
    public double halfWidth() {
        return ridden.halfWidth();
    }

    @Override
    public double height() {
        return ridden.height();
    }

    @Override
    public double stepHeight() {
        return 0.0;
    }

    @Override
    public ShapeQuery shapeQuery(double feetY, boolean deepFall) {
        return new ShapeQuery(feetY, false, false, false);
    }

    public FlyingModel flying() {
        return flying;
    }
}
