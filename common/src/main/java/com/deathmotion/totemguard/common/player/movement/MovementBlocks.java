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

package com.deathmotion.totemguard.common.player.movement;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;

public final class MovementBlocks {

    private MovementBlocks() {
    }

    public static boolean isFluid(WrappedBlockState state) {
        StateType type = state.getType();
        if (type == StateTypes.WATER || type == StateTypes.LAVA) return true;
        return state.hasProperty(StateValue.WATERLOGGED) && state.isWaterlogged();
    }

    public static boolean isFluidType(StateType type) {
        return type == StateTypes.WATER || type == StateTypes.LAVA;
    }

    public static boolean isStuck(StateType type) {
        return type == StateTypes.COBWEB || type == StateTypes.POWDER_SNOW || type == StateTypes.SWEET_BERRY_BUSH;
    }

    public static boolean isClimbable(StateType type) {
        return type == StateTypes.LADDER
                || type == StateTypes.VINE
                || type == StateTypes.SCAFFOLDING
                || type == StateTypes.TWISTING_VINES
                || type == StateTypes.TWISTING_VINES_PLANT
                || type == StateTypes.WEEPING_VINES
                || type == StateTypes.WEEPING_VINES_PLANT
                || type == StateTypes.CAVE_VINES
                || type == StateTypes.CAVE_VINES_PLANT;
    }

    public static boolean isSolidSupport(StateType type) {
        return type != StateTypes.AIR && type.isBlocking() && !isFluidType(type);
    }
}
