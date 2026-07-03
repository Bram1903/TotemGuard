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

public final class StuckBlocks {

    private StuckBlocks() {
    }

    public static boolean isStuck(StateType type) {
        return type == StateTypes.COBWEB || type == StateTypes.POWDER_SNOW || type == StateTypes.SWEET_BERRY_BUSH;
    }

    public static double horizontal(StateType type) {
        if (type == StateTypes.COBWEB) return 0.25;
        if (type == StateTypes.SWEET_BERRY_BUSH) return 0.8;
        if (type == StateTypes.POWDER_SNOW) return 0.9;
        return 1.0;
    }

    public static double vertical(StateType type) {
        if (type == StateTypes.COBWEB) return 0.05;
        if (type == StateTypes.SWEET_BERRY_BUSH) return 0.75;
        if (type == StateTypes.POWDER_SNOW) return 1.5;
        return 1.0;
    }
}
