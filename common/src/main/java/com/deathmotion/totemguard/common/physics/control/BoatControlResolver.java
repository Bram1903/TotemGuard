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

package com.deathmotion.totemguard.common.physics.control;

import com.deathmotion.totemguard.common.physics.VersionGates;
import com.deathmotion.totemguard.common.physics.medium.model.BoatFloatModel;
import com.deathmotion.totemguard.common.util.ClientMath;

public final class BoatControlResolver {

    private BoatControlResolver() {
    }

    public static BoatControl build(VersionGates gates, float yaw) {
        boolean modernTrig = gates.modernTrig();
        return new BoatControl(
                ClientMath.lookX(yaw, 0.0f, modernTrig),
                ClientMath.lookZ(yaw, 0.0f, modernTrig),
                BoatFloatModel.CONTROL_MIN,
                BoatFloatModel.CONTROL_MAX);
    }
}
