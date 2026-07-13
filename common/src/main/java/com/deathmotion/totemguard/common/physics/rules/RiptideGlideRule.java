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

package com.deathmotion.totemguard.common.physics.rules;

import com.deathmotion.totemguard.common.physics.area.AreaBounds;
import com.deathmotion.totemguard.common.physics.area.MotionArea;
import com.deathmotion.totemguard.common.physics.collision.ContactReport;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSelect;
import com.deathmotion.totemguard.common.player.data.Data;

public final class RiptideGlideRule {

    private final AreaBounds probe = new AreaBounds();

    public void offer(MediumModel medium, Data data, MediumSelect mediums,
                      ControlEnvelope input, GroundFacts ground, ContactReport contact,
                      MotionArea carried, AreaBounds bounds) {
        if (medium.kind() != MediumKind.GLIDE || !data.getGlideData().riptideActive()) return;
        double strength = data.getGlideData().riptideStrength();
        double impulseY = input.lookY() * strength;
        probe.reset(carried);
        probe.centerX(carried.centerX() + input.lookX() * strength);
        probe.centerZ(carried.centerZ() + input.lookZ() * strength);
        probe.floor(carried.floorVy() + impulseY);
        probe.ceiling(carried.ceilVy() + impulseY);
        mediums.glide().horizontalOptions(input, ground, probe);
        mediums.glide().verticalOptions(input, ground, contact, probe);
        bounds.altCenter(probe.centerX(), probe.centerZ());
        bounds.expandRadius(probe.radius());
        bounds.raiseCeiling(probe.ceiling());
        if (impulseY < 0.0) bounds.lowerFloor(probe.floor());
    }
}
