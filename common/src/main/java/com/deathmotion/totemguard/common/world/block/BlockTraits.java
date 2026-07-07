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
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;

public final class BlockTraits {

    public static final double DEFAULT_SLIPPERINESS = 0.6;
    public static final double SLIME_SLIPPERINESS = 0.8;
    public static final double ICE_SLIPPERINESS = 0.98;
    public static final double BLUE_ICE_SLIPPERINESS = 0.989;

    public static final double SLOWING_SPEED_FACTOR = 0.4;
    public static final double HALF_JUMP_FACTOR = 0.5;

    public static final double SLIME_BOUNCE = 1.0;
    public static final double BED_BOUNCE = 0.66;

    public static final double BUBBLE_COLUMN_SURFACE_ASCENT = 1.8;
    public static final double BUBBLE_COLUMN_INSIDE_ASCENT = 0.7;

    private static final double SOURCE_FLUID_HEIGHT = 8.0 / 9.0;

    private BlockTraits() {
    }

    public static double slipperiness(StateType type) {
        if (type == StateTypes.ICE || type == StateTypes.PACKED_ICE || type == StateTypes.FROSTED_ICE) {
            return ICE_SLIPPERINESS;
        }
        if (type == StateTypes.BLUE_ICE) return BLUE_ICE_SLIPPERINESS;
        if (type == StateTypes.SLIME_BLOCK) return SLIME_SLIPPERINESS;
        return DEFAULT_SLIPPERINESS;
    }

    public static double speedFactor(StateType type) {
        if (type == StateTypes.SOUL_SAND || type == StateTypes.HONEY_BLOCK) return SLOWING_SPEED_FACTOR;
        return 1.0;
    }

    public static double jumpFactor(StateType type) {
        if (type == StateTypes.HONEY_BLOCK) return HALF_JUMP_FACTOR;
        return 1.0;
    }

    public static double bounceFactor(StateType type) {
        if (type == StateTypes.SLIME_BLOCK) return SLIME_BOUNCE;
        if (BlockTags.BEDS.contains(type)) return BED_BOUNCE;
        return 0.0;
    }

    public static boolean isFluid(WrappedBlockState state) {
        StateType type = state.getType();
        if (type == StateTypes.WATER || type == StateTypes.LAVA || type == StateTypes.BUBBLE_COLUMN) return true;
        if (type == StateTypes.KELP || type == StateTypes.KELP_PLANT
                || type == StateTypes.SEAGRASS || type == StateTypes.TALL_SEAGRASS) {
            return true;
        }
        return state.hasProperty(StateValue.WATERLOGGED) && state.isWaterlogged();
    }

    public static boolean isBareFluid(StateType type) {
        return type == StateTypes.WATER || type == StateTypes.LAVA;
    }

    public static boolean isBubbleColumn(StateType type) {
        return type == StateTypes.BUBBLE_COLUMN;
    }

    public static double fluidSurfaceHeight(WrappedBlockState state) {
        StateType type = state.getType();
        if (type == StateTypes.WATER || type == StateTypes.LAVA) {
            int level = state.getLevel();
            if (level >= 1 && level <= 7) return (8 - level) / 9.0;
        }
        return SOURCE_FLUID_HEIGHT;
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
        if (!BlockTags.TRAPDOORS.contains(trapdoor.getType()) || !trapdoor.isOpen()) return false;
        if (below.getType() != StateTypes.LADDER) return false;
        return trapdoor.getFacing() == below.getFacing();
    }
}
