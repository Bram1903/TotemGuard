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

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;

public final class FluidBlocks {

    public static final double BUBBLE_COLUMN_SURFACE_ASCENT = 1.8;
    public static final double BUBBLE_COLUMN_INSIDE_ASCENT = 0.7;

    private static final double SOURCE_HEIGHT = 8.0 / 9.0;

    private FluidBlocks() {
    }

    public static boolean isFluid(WrappedBlockState state) {
        StateType type = state.getType();
        if (type == StateTypes.WATER || type == StateTypes.LAVA || type == StateTypes.BUBBLE_COLUMN) return true;
        return state.hasProperty(StateValue.WATERLOGGED) && state.isWaterlogged();
    }

    public static boolean isFluidType(StateType type) {
        return type == StateTypes.WATER || type == StateTypes.LAVA;
    }

    public static boolean isBubbleColumn(StateType type) {
        return type == StateTypes.BUBBLE_COLUMN;
    }

    public static double surfaceHeight(WrappedBlockState state) {
        StateType type = state.getType();
        if (type == StateTypes.WATER || type == StateTypes.LAVA) {
            int level = state.getLevel();
            if (level >= 1 && level <= 7) return (8 - level) / 9.0;
        }
        return SOURCE_HEIGHT;
    }
}
