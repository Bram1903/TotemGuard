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

package com.deathmotion.totemguard.common.world.block;

import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

public final class Slipperiness {

    public static final double DEFAULT = 0.6;

    private static final double ICE = 0.98;
    private static final double BLUE_ICE = 0.989;
    private static final double SLIME = 0.8;

    private Slipperiness() {
    }

    public static double of(StateType type) {
        if (type == StateTypes.ICE || type == StateTypes.PACKED_ICE || type == StateTypes.FROSTED_ICE) {
            return ICE;
        }
        if (type == StateTypes.BLUE_ICE) return BLUE_ICE;
        if (type == StateTypes.SLIME_BLOCK) return SLIME;
        return DEFAULT;
    }
}
