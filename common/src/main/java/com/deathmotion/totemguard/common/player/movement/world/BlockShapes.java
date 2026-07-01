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
import com.github.retrooper.packetevents.protocol.world.states.enums.Type;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

public final class BlockShapes {

    private static final double FENCE_TOP = 1.5;
    private static final double SLAB_BOTTOM_TOP = 0.5;
    private static final double CARPET_TOP = 0.0625;
    private static final double SNOW_LAYER_STEP = 0.125;
    private static final double BED_TOP = 0.5625;
    private static final double LILY_PAD_TOP = 0.09375;
    private static final double SCAFFOLD_STABLE_TOP = 1.0;
    private static final double ABOVE_EPS = 1.0e-5;

    private BlockShapes() {
    }

    public static CollisionShape shapeOf(WrappedBlockState state, int cellY, CollisionContext ctx) {
        StateType type = state.getType();
        if (type == StateTypes.AIR || MovementBlocks.isFluidType(type)) return CollisionShape.EMPTY;

        if (type == StateTypes.SCAFFOLDING) return scaffolding(cellY, ctx);
        if (type == StateTypes.POWDER_SNOW) return CollisionShape.FULL;
        if (type == StateTypes.LILY_PAD) return CollisionShape.top(LILY_PAD_TOP);
        if (type == StateTypes.SNOW) return CollisionShape.top(Math.max(1, state.getLayers()) * SNOW_LAYER_STEP);

        if (BlockTags.FENCES.contains(type) || BlockTags.FENCE_GATES.contains(type) || BlockTags.WALLS.contains(type)) {
            return CollisionShape.top(FENCE_TOP);
        }
        if (BlockTags.SLABS.contains(type)) {
            return CollisionShape.top(state.getTypeData() == Type.BOTTOM ? SLAB_BOTTOM_TOP : 1.0);
        }
        if (BlockTags.WOOL_CARPETS.contains(type) || type == StateTypes.MOSS_CARPET || type == StateTypes.PALE_MOSS_CARPET) {
            return CollisionShape.top(CARPET_TOP);
        }
        if (BlockTags.TRAPDOORS.contains(type)) {
            return state.isOpen() ? CollisionShape.EMPTY : CollisionShape.FULL;
        }
        if (BlockTags.GLASS_PANES.contains(type) || BlockTags.BARS.contains(type)) {
            return CollisionShape.FULL;
        }
        if (BlockTags.BEDS.contains(type)) {
            return CollisionShape.top(BED_TOP);
        }
        if (BlockTags.STAIRS.contains(type)) {
            return CollisionShape.FULL;
        }

        return type.isBlocking() ? CollisionShape.FULL : CollisionShape.EMPTY;
    }

    private static CollisionShape scaffolding(int cellY, CollisionContext ctx) {
        boolean above = ctx.feetY() > cellY + SCAFFOLD_STABLE_TOP - ABOVE_EPS;
        if (above && !ctx.descending()) return CollisionShape.top(SCAFFOLD_STABLE_TOP);
        return CollisionShape.EMPTY;
    }
}
