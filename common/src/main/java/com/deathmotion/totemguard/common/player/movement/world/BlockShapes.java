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
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Type;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.Set;

public final class BlockShapes {

    private static final double FENCE_TOP = 1.5;
    private static final double SLAB_BOTTOM_TOP = 0.5;
    private static final double CARPET_TOP = 0.0625;
    private static final double SNOW_LAYER_STEP = 0.125;
    private static final double BED_TOP = 0.5625;
    private static final double SOUL_SAND_TOP = 0.875;
    private static final double ENCHANTING_TABLE_TOP = 0.75;
    private static final double DAYLIGHT_DETECTOR_TOP = 0.375;
    private static final double PATH_TOP = 0.9375;
    private static final double PORTAL_FRAME_TOP = 0.8125;
    private static final double CAKE_TOP = 0.5;
    private static final double TRAPDOOR_THICKNESS = 0.1875;
    private static final double LADDER_THICKNESS = 0.1875;
    private static final double SCAFFOLD_STABLE_TOP = 1.0;
    private static final double ABOVE_EPS = 1.0e-5;
    private static final double INSET = 0.0625;
    private static final double INSET_MAX = 0.9375;
    private static final double SKULL_INSET = 0.25;
    private static final double SKULL_TOP = 0.5;

    private static final Set<StateType> FLOOR_SKULLS = Set.of(
            StateTypes.SKELETON_SKULL, StateTypes.WITHER_SKELETON_SKULL, StateTypes.ZOMBIE_HEAD,
            StateTypes.PLAYER_HEAD, StateTypes.CREEPER_HEAD, StateTypes.DRAGON_HEAD, StateTypes.PIGLIN_HEAD);

    private static final Set<StateType> CAULDRONS = Set.of(
            StateTypes.CAULDRON, StateTypes.WATER_CAULDRON, StateTypes.LAVA_CAULDRON, StateTypes.POWDER_SNOW_CAULDRON);

    private BlockShapes() {
    }

    public static CollisionShape shapeOf(WrappedBlockState state, int cellY, CollisionContext ctx) {
        StateType type = state.getType();
        if (type == StateTypes.AIR || MovementBlocks.isFluidType(type)) return CollisionShape.EMPTY;

        if (type == StateTypes.SCAFFOLDING) return scaffolding(cellY, ctx);
        if (type == StateTypes.POWDER_SNOW) return CollisionShape.FULL;
        if (type == StateTypes.SNOW) return snowLayers(state);
        if (type == StateTypes.LILY_PAD) return inset(0.09375);

        if (type == StateTypes.SOUL_SAND) return CollisionShape.top(SOUL_SAND_TOP);
        if (type == StateTypes.HONEY_BLOCK) return inset(INSET_MAX);
        if (type == StateTypes.CACTUS) return inset(INSET_MAX);
        if (type == StateTypes.ENCHANTING_TABLE) return CollisionShape.top(ENCHANTING_TABLE_TOP);
        if (type == StateTypes.DAYLIGHT_DETECTOR) return CollisionShape.top(DAYLIGHT_DETECTOR_TOP);
        if (type == StateTypes.FARMLAND || type == StateTypes.DIRT_PATH || type == StateTypes.GRASS_PATH) {
            return CollisionShape.top(PATH_TOP);
        }
        if (type == StateTypes.CHEST || type == StateTypes.TRAPPED_CHEST || type == StateTypes.ENDER_CHEST) {
            return inset(SOUL_SAND_TOP);
        }
        if (type == StateTypes.END_PORTAL_FRAME) return endPortalFrame(state);
        if (type == StateTypes.BREWING_STAND) return brewingStand();
        if (type == StateTypes.CAKE) return CollisionShape.box(INSET, 0.0, INSET, INSET_MAX, CAKE_TOP, INSET_MAX);
        if (FLOOR_SKULLS.contains(type)) return CollisionShape.box(SKULL_INSET, 0.0, SKULL_INSET, 1.0 - SKULL_INSET, SKULL_TOP, 1.0 - SKULL_INSET);
        if (type == StateTypes.LADDER) return ladder(state);
        if (CAULDRONS.contains(type)) return cauldron();

        if (BlockTags.FENCES.contains(type) || BlockTags.FENCE_GATES.contains(type) || BlockTags.WALLS.contains(type)) {
            return CollisionShape.top(FENCE_TOP);
        }
        if (BlockTags.SLABS.contains(type)) {
            return state.getTypeData() == Type.BOTTOM ? CollisionShape.top(SLAB_BOTTOM_TOP) : CollisionShape.FULL;
        }
        if (BlockTags.WOOL_CARPETS.contains(type) || type == StateTypes.MOSS_CARPET || type == StateTypes.PALE_MOSS_CARPET) {
            return CollisionShape.top(CARPET_TOP);
        }
        if (BlockTags.TRAPDOORS.contains(type)) {
            if (state.isOpen()) return CollisionShape.EMPTY;
            return state.getHalf() == Half.BOTTOM ? CollisionShape.top(TRAPDOOR_THICKNESS) : CollisionShape.FULL;
        }
        if (BlockTags.GLASS_PANES.contains(type) || BlockTags.BARS.contains(type)) {
            return CollisionShape.FULL;
        }
        if (BlockTags.BEDS.contains(type)) {
            return CollisionShape.top(BED_TOP);
        }
        if (BlockTags.STAIRS.contains(type)) {
            return state.getHalf() == Half.TOP ? CollisionShape.FULL : stairsBottom();
        }

        return type.isBlocking() ? CollisionShape.FULL : CollisionShape.EMPTY;
    }

    private static CollisionShape snowLayers(WrappedBlockState state) {
        int layers = Math.max(1, state.getLayers());
        if (layers <= 1) return CollisionShape.EMPTY;
        return CollisionShape.top((layers - 1) * SNOW_LAYER_STEP);
    }

    private static CollisionShape inset(double top) {
        return CollisionShape.box(INSET, 0.0, INSET, INSET_MAX, top, INSET_MAX);
    }

    private static CollisionShape endPortalFrame(WrappedBlockState state) {
        if (!state.isEye()) return CollisionShape.top(PORTAL_FRAME_TOP);
        return CollisionShape.of(
                new CollisionBox(0.0, 0.0, 0.0, 1.0, PORTAL_FRAME_TOP, 1.0),
                new CollisionBox(0.25, PORTAL_FRAME_TOP, 0.25, 0.75, 1.0, 0.75));
    }

    private static CollisionShape brewingStand() {
        return CollisionShape.of(
                new CollisionBox(INSET, 0.0, INSET, INSET_MAX, 0.125, INSET_MAX),
                new CollisionBox(0.4375, 0.0, 0.4375, 0.5625, 0.875, 0.5625));
    }

    private static CollisionShape stairsBottom() {
        return CollisionShape.of(
                new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                new CollisionBox(0.0, 0.5, 0.0, 1.0, 1.0, 1.0));
    }

    private static CollisionShape cauldron() {
        return CollisionShape.of(
                new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.25, 1.0),
                new CollisionBox(0.0, 0.25, 0.0, 0.125, 1.0, 1.0),
                new CollisionBox(0.875, 0.25, 0.0, 1.0, 1.0, 1.0),
                new CollisionBox(0.0, 0.25, 0.0, 1.0, 1.0, 0.125),
                new CollisionBox(0.0, 0.25, 0.875, 1.0, 1.0, 1.0));
    }

    private static CollisionShape ladder(WrappedBlockState state) {
        double far = 1.0 - LADDER_THICKNESS;
        return switch (state.getFacing()) {
            case NORTH -> CollisionShape.box(0.0, 0.0, far, 1.0, 1.0, 1.0);
            case SOUTH -> CollisionShape.box(0.0, 0.0, 0.0, 1.0, 1.0, LADDER_THICKNESS);
            case WEST -> CollisionShape.box(far, 0.0, 0.0, 1.0, 1.0, 1.0);
            case EAST -> CollisionShape.box(0.0, 0.0, 0.0, LADDER_THICKNESS, 1.0, 1.0);
            default -> CollisionShape.EMPTY;
        };
    }

    private static CollisionShape scaffolding(int cellY, CollisionContext ctx) {
        boolean above = ctx.feetY() > cellY + SCAFFOLD_STABLE_TOP - ABOVE_EPS;
        if (above && !ctx.descending()) return CollisionShape.top(SCAFFOLD_STABLE_TOP);
        return CollisionShape.EMPTY;
    }
}
