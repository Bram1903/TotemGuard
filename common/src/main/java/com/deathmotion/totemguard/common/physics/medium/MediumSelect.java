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

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class MediumSelect {

    @Getter
    private final LandModel land = new LandModel();
    @Getter
    private final WaterModel water = new WaterModel();
    private final LavaModel lava = new LavaModel();
    private final ClimbModel climb = new ClimbModel();

    public MediumModel select(MediumSample sample) {
        if (sample.water()) return water;
        if (sample.lava()) return lava;
        if (sample.climbable() && !sample.stuck()) return climb;
        return land;
    }

    public void reset() {
        water.reset();
    }
}
