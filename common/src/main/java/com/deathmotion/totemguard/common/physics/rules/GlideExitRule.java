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
import com.deathmotion.totemguard.common.physics.ground.GroundFacts;
import com.deathmotion.totemguard.common.physics.control.ControlEnvelope;
import com.deathmotion.totemguard.common.physics.medium.MediumKind;
import com.deathmotion.totemguard.common.physics.medium.MediumModel;
import com.deathmotion.totemguard.common.physics.medium.MediumSelect;
import com.deathmotion.totemguard.common.physics.medium.model.LandModel;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.util.ClientMath;

public final class GlideExitRule {

    private final AreaBounds probe = new AreaBounds();

    public void widenForExit(MediumModel medium, Data data, MediumSelect mediums,
                             ControlEnvelope input, GroundFacts ground,
                             MotionArea carried, AreaBounds bounds) {
        if (medium.kind() != MediumKind.LAND || !data.getGlideData().justExited()) return;
        probe.reset(carried);
        mediums.glide().prepare(false, false, data.getFireworkData());
        mediums.glide().horizontalOptions(input, ground, probe);
        double reachX = probe.centerX() - bounds.centerX();
        double reachZ = probe.centerZ() - bounds.centerZ();
        bounds.expandRadius(ClientMath.horizontalDistance(reachX, reachZ) + probe.radius());
    }

    public MotionArea widenExitDecay(MediumModel medium, MediumSelect mediums, Data data,
                                     double frictionMax, double speedFactor,
                                     AreaBounds bounds, MotionArea carried) {
        if (medium != mediums.land() || !data.getGlideData().exitActive()) return carried;
        double groundDecay = frictionMax * speedFactor;
        double airDecay = LandModel.AIR_FRICTION * speedFactor;
        if (airDecay <= groundDecay) return carried;
        double span = ClientMath.horizontalDistance(bounds.legalX(), bounds.legalZ())
                * (airDecay - groundDecay);
        return new MotionArea(carried.centerX(), carried.centerZ(),
                carried.slack() + span, carried.floorVy(), carried.ceilVy());
    }
}
