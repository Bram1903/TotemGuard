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

import com.deathmotion.totemguard.common.physics.control.RiderControlResolver;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.physics.medium.model.RiderWaterModel;
import com.deathmotion.totemguard.common.physics.medium.model.StriderLavaModel;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

public final class LivingVehicleBody implements SimulationBody {

    private final RiderWaterModel water = new RiderWaterModel();
    private final StriderLavaModel lava;

    private TrackedEntity ridden;
    private BodyKind kind = BodyKind.HORSE;

    public LivingVehicleBody(LandModel land) {
        this.lava = new StriderLavaModel(land);
    }

    public void mount(TrackedEntity ridden, EntityType type) {
        this.ridden = ridden;
        if (EntityRoles.camel(type)) {
            kind = BodyKind.CAMEL;
        } else if (type == EntityTypes.STRIDER) {
            kind = BodyKind.STRIDER;
        } else if (type == EntityTypes.PIG) {
            kind = BodyKind.PIG;
        } else {
            kind = BodyKind.HORSE;
        }
    }

    @Override
    public BodyKind kind() {
        return kind;
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
        return RiderControlResolver.stepHeight(ridden, kind == BodyKind.CAMEL);
    }

    @Override
    public ShapeQuery shapeQuery(double feetY, boolean deepFall) {
        return new ShapeQuery(feetY, false, false, false);
    }

    public RiderWaterModel water() {
        return water;
    }

    public StriderLavaModel lava() {
        return lava;
    }
}
