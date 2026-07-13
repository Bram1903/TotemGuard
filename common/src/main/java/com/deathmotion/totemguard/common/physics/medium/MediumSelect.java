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

import com.deathmotion.totemguard.common.physics.medium.model.*;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class MediumSelect {

    @Getter
    private final LandModel land = new LandModel();
    @Getter
    private final WaterModel water = new WaterModel();
    @Getter
    private final GlideModel glide = new GlideModel(land);
    @Getter
    private final FlyModel fly = new FlyModel();
    private final LavaModel lava = new LavaModel();
    private final ClimbModel climb = new ClimbModel();

    public MediumModel select(MediumSample sample, GlideState glideState,
                              boolean glideYieldsToClimb, boolean flying) {
        if (flying) return fly;
        if (sample.water()) {
            water.observe(sample);
            return water;
        }
        if (sample.lava()) return lava;
        boolean glidesPastClimbable = glideState == GlideState.FLAG && !glideYieldsToClimb;
        if (sample.climbable() && !sample.stuck() && !glidesPastClimbable) return climb;
        if (glideState != GlideState.NONE) return glide;
        return land;
    }

    public void reset() {
        water.reset();
        glide.reset();
    }
}
