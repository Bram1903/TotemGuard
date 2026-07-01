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

package com.deathmotion.totemguard.common.player.movement.world;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;

public final class MovementBlocks {

    private static final double DEFAULT_SLIPPERINESS = 0.6;
    private static final double ICE_SLIPPERINESS = 0.98;
    private static final double BLUE_ICE_SLIPPERINESS = 0.989;
    private static final double SLIME_SLIPPERINESS = 0.8;

    private MovementBlocks() {
    }

    public static boolean isBouncy(StateType type) {
        return type == StateTypes.SLIME_BLOCK || BlockTags.BEDS.contains(type);
    }

    public static double slipperiness(StateType type) {
        if (type == StateTypes.ICE || type == StateTypes.PACKED_ICE || type == StateTypes.FROSTED_ICE) {
            return ICE_SLIPPERINESS;
        }
        if (type == StateTypes.BLUE_ICE) return BLUE_ICE_SLIPPERINESS;
        if (type == StateTypes.SLIME_BLOCK) return SLIME_SLIPPERINESS;
        return DEFAULT_SLIPPERINESS;
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

    public static double stuckHorizontal(StateType type) {
        if (type == StateTypes.COBWEB) return 0.25;
        if (type == StateTypes.SWEET_BERRY_BUSH) return 0.8;
        if (type == StateTypes.POWDER_SNOW) return 0.9;
        return 1.0;
    }

    public static double stuckVertical(StateType type) {
        if (type == StateTypes.COBWEB) return 0.05;
        if (type == StateTypes.SWEET_BERRY_BUSH) return 0.75;
        if (type == StateTypes.POWDER_SNOW) return 1.5;
        return 1.0;
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

    public static boolean trapdoorUsableAsLadder(WrappedBlockState trapdoor, WrappedBlockState below) {
        if (!isTrapdoor(trapdoor.getType()) || !trapdoor.isOpen()) return false;
        if (below.getType() != StateTypes.LADDER) return false;
        return trapdoor.getFacing() == below.getFacing();
    }

    private static boolean isTrapdoor(StateType type) {
        return BlockTags.TRAPDOORS.contains(type);
    }
}
