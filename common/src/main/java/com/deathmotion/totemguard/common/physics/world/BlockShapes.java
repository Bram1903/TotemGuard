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

package com.deathmotion.totemguard.common.physics.world;

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.defaulttags.BlockTags;
import com.github.retrooper.packetevents.protocol.world.states.enums.East;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Hinge;
import com.github.retrooper.packetevents.protocol.world.states.enums.North;
import com.github.retrooper.packetevents.protocol.world.states.enums.South;
import com.github.retrooper.packetevents.protocol.world.states.enums.Type;
import com.github.retrooper.packetevents.protocol.world.states.enums.West;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.ArrayList;
import java.util.List;
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
    private static final double DOOR_THICKNESS = 0.1875;
    private static final double FENCE_ARM_HALF = 0.125;
    private static final double PANE_ARM_HALF = 0.0625;
    private static final double WALL_POST_HALF = 0.25;
    private static final double WALL_ARM_HALF = 0.1875;
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

    private static final Set<StateType> GOLEM_STATUES = Set.of(
            StateTypes.COPPER_GOLEM_STATUE, StateTypes.EXPOSED_COPPER_GOLEM_STATUE,
            StateTypes.WEATHERED_COPPER_GOLEM_STATUE, StateTypes.OXIDIZED_COPPER_GOLEM_STATUE,
            StateTypes.WAXED_COPPER_GOLEM_STATUE, StateTypes.WAXED_EXPOSED_COPPER_GOLEM_STATUE,
            StateTypes.WAXED_WEATHERED_COPPER_GOLEM_STATUE, StateTypes.WAXED_OXIDIZED_COPPER_GOLEM_STATUE);

    private static final CollisionShape DOOR_NORTH = CollisionShape.box(0.0, 0.0, 1.0 - DOOR_THICKNESS, 1.0, 1.0, 1.0);
    private static final CollisionShape DOOR_SOUTH = CollisionShape.box(0.0, 0.0, 0.0, 1.0, 1.0, DOOR_THICKNESS);
    private static final CollisionShape DOOR_WEST = CollisionShape.box(1.0 - DOOR_THICKNESS, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final CollisionShape DOOR_EAST = CollisionShape.box(0.0, 0.0, 0.0, DOOR_THICKNESS, 1.0, 1.0);

    private static final int[] STAIR_QUADRANTS_BY_STATE = {12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    private static final Set<StateType> WALL_UNTRUSTED = Set.of(
            StateTypes.POWDER_SNOW, StateTypes.SCAFFOLDING,
            StateTypes.BAMBOO, StateTypes.CHORUS_PLANT, StateTypes.DRAGON_EGG,
            StateTypes.TURTLE_EGG, StateTypes.SNIFFER_EGG,
            StateTypes.SCULK_SENSOR, StateTypes.CALIBRATED_SCULK_SENSOR, StateTypes.SCULK_SHRIEKER,
            StateTypes.HOPPER, StateTypes.COMPOSTER, StateTypes.STONECUTTER,
            StateTypes.BELL, StateTypes.GRINDSTONE, StateTypes.LECTERN,
            StateTypes.LANTERN, StateTypes.SOUL_LANTERN, StateTypes.CHAIN,
            StateTypes.END_ROD, StateTypes.LIGHTNING_ROD,
            StateTypes.FLOWER_POT, StateTypes.DECORATED_POT,
            StateTypes.POINTED_DRIPSTONE, StateTypes.BIG_DRIPLEAF,
            StateTypes.AZALEA, StateTypes.FLOWERING_AZALEA,
            StateTypes.AMETHYST_CLUSTER, StateTypes.LARGE_AMETHYST_BUD,
            StateTypes.MEDIUM_AMETHYST_BUD, StateTypes.SMALL_AMETHYST_BUD,
            StateTypes.CONDUIT, StateTypes.PISTON_HEAD, StateTypes.MOVING_PISTON,
            StateTypes.SKELETON_WALL_SKULL, StateTypes.WITHER_SKELETON_WALL_SKULL,
            StateTypes.ZOMBIE_WALL_HEAD, StateTypes.PLAYER_WALL_HEAD,
            StateTypes.CREEPER_WALL_HEAD, StateTypes.DRAGON_WALL_HEAD, StateTypes.PIGLIN_WALL_HEAD);

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

        if (BlockTags.FENCES.contains(type)) return fence(state);
        if (BlockTags.FENCE_GATES.contains(type)) return fenceGate(state);
        if (BlockTags.WALLS.contains(type)) return wall(state);
        if (BlockTags.SLABS.contains(type)) {
            return state.getTypeData() == Type.BOTTOM ? CollisionShape.top(SLAB_BOTTOM_TOP) : CollisionShape.FULL;
        }
        if (BlockTags.WOOL_CARPETS.contains(type) || type == StateTypes.MOSS_CARPET || type == StateTypes.PALE_MOSS_CARPET) {
            return CollisionShape.top(CARPET_TOP);
        }
        if (BlockTags.TRAPDOORS.contains(type)) {
            if (state.isOpen()) return trapdoorOpen(state);
            return state.getHalf() == Half.BOTTOM
                    ? CollisionShape.top(TRAPDOOR_THICKNESS)
                    : CollisionShape.box(0.0, 1.0 - TRAPDOOR_THICKNESS, 0.0, 1.0, 1.0, 1.0);
        }
        if (BlockTags.GLASS_PANES.contains(type) || BlockTags.BARS.contains(type)) return pane(state);
        if (BlockTags.BEDS.contains(type)) {
            return CollisionShape.top(BED_TOP);
        }
        if (BlockTags.STAIRS.contains(type)) return stairs(state);
        if (BlockTags.DOORS.contains(type)) return door(state);
        if (type == StateTypes.HEAVY_CORE) return CollisionShape.box(0.25, 0.0, 0.25, 0.75, 0.5, 0.75);
        if (GOLEM_STATUES.contains(type)) return CollisionShape.box(0.1875, 0.0, 0.1875, 0.8125, 0.875, 0.8125);

        return type.isBlocking() && type.isSolid() ? CollisionShape.FULL : CollisionShape.EMPTY;
    }

    public static boolean wallTrusted(StateType type) {
        if (WALL_UNTRUSTED.contains(type)) return false;
        if (BlockTags.ANVIL.contains(type)) return false;
        if (BlockTags.CANDLES.contains(type) || BlockTags.CANDLE_CAKES.contains(type)) return false;
        return !BlockTags.CAMPFIRES.contains(type);
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

    private static CollisionShape cross(boolean post, double postHalf, double armHalf, double height,
                                        boolean north, boolean south, boolean west, boolean east) {
        List<CollisionBox> boxes = new ArrayList<>(5);
        double p0 = 0.5 - postHalf, p1 = 0.5 + postHalf;
        double a0 = 0.5 - armHalf, a1 = 0.5 + armHalf;
        if (post) boxes.add(new CollisionBox(p0, 0.0, p0, p1, height, p1));
        if (north) boxes.add(new CollisionBox(a0, 0.0, 0.0, a1, height, a1));
        if (south) boxes.add(new CollisionBox(a0, 0.0, a0, a1, height, 1.0));
        if (west) boxes.add(new CollisionBox(0.0, 0.0, a0, a1, height, a1));
        if (east) boxes.add(new CollisionBox(a0, 0.0, a0, 1.0, height, a1));
        if (boxes.isEmpty()) return CollisionShape.EMPTY;
        return CollisionShape.of(boxes.toArray(new CollisionBox[0]));
    }

    private static CollisionShape fence(WrappedBlockState state) {
        return cross(true, FENCE_ARM_HALF, FENCE_ARM_HALF, FENCE_TOP,
                state.getNorth() != North.FALSE, state.getSouth() != South.FALSE,
                state.getWest() != West.FALSE, state.getEast() != East.FALSE);
    }

    private static CollisionShape pane(WrappedBlockState state) {
        return cross(true, PANE_ARM_HALF, PANE_ARM_HALF, 1.0,
                state.getNorth() != North.FALSE, state.getSouth() != South.FALSE,
                state.getWest() != West.FALSE, state.getEast() != East.FALSE);
    }

    private static CollisionShape wall(WrappedBlockState state) {
        return cross(state.isUp(), WALL_POST_HALF, WALL_ARM_HALF, FENCE_TOP,
                state.getNorth() != North.NONE && state.getNorth() != North.FALSE,
                state.getSouth() != South.NONE && state.getSouth() != South.FALSE,
                state.getWest() != West.NONE && state.getWest() != West.FALSE,
                state.getEast() != East.NONE && state.getEast() != East.FALSE);
    }

    private static CollisionShape fenceGate(WrappedBlockState state) {
        if (state.isOpen()) return CollisionShape.EMPTY;
        return switch (state.getFacing()) {
            case NORTH, SOUTH -> CollisionShape.box(0.0, 0.0, 0.5 - FENCE_ARM_HALF, 1.0, FENCE_TOP, 0.5 + FENCE_ARM_HALF);
            default -> CollisionShape.box(0.5 - FENCE_ARM_HALF, 0.0, 0.0, 0.5 + FENCE_ARM_HALF, FENCE_TOP, 1.0);
        };
    }

    private static CollisionShape door(WrappedBlockState state) {
        boolean closed = !state.isOpen();
        boolean rightHinge = state.getHinge() == Hinge.RIGHT;
        return switch (state.getFacing()) {
            case SOUTH -> closed ? DOOR_SOUTH : (rightHinge ? DOOR_EAST : DOOR_WEST);
            case WEST -> closed ? DOOR_WEST : (rightHinge ? DOOR_SOUTH : DOOR_NORTH);
            case NORTH -> closed ? DOOR_NORTH : (rightHinge ? DOOR_WEST : DOOR_EAST);
            default -> closed ? DOOR_EAST : (rightHinge ? DOOR_NORTH : DOOR_SOUTH);
        };
    }

    private static CollisionShape trapdoorOpen(WrappedBlockState state) {
        double far = 1.0 - TRAPDOOR_THICKNESS;
        return switch (state.getFacing()) {
            case NORTH -> CollisionShape.box(0.0, 0.0, far, 1.0, 1.0, 1.0);
            case SOUTH -> CollisionShape.box(0.0, 0.0, 0.0, 1.0, 1.0, TRAPDOOR_THICKNESS);
            case WEST -> CollisionShape.box(far, 0.0, 0.0, 1.0, 1.0, 1.0);
            case EAST -> CollisionShape.box(0.0, 0.0, 0.0, TRAPDOOR_THICKNESS, 1.0, 1.0);
            default -> CollisionShape.EMPTY;
        };
    }

    private static CollisionShape stairs(WrappedBlockState state) {
        int facing = switch (state.getFacing()) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> -1;
        };
        if (facing < 0) return CollisionShape.FULL;
        int shape = switch (state.getShape()) {
            case INNER_LEFT -> 1;
            case INNER_RIGHT -> 2;
            case OUTER_LEFT -> 3;
            case OUTER_RIGHT -> 4;
            default -> 0;
        };
        boolean top = state.getHalf() == Half.TOP;
        double stepY0 = top ? 0.0 : 0.5;
        double stepY1 = top ? 0.5 : 1.0;
        int quadrants = STAIR_QUADRANTS_BY_STATE[shape * 4 + facing];

        List<CollisionBox> boxes = new ArrayList<>(4);
        boxes.add(top
                ? new CollisionBox(0.0, 0.5, 0.0, 1.0, 1.0, 1.0)
                : new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.5, 1.0));
        if ((quadrants & 1) != 0) boxes.add(new CollisionBox(0.0, stepY0, 0.0, 0.5, stepY1, 0.5));
        if ((quadrants & 2) != 0) boxes.add(new CollisionBox(0.5, stepY0, 0.0, 1.0, stepY1, 0.5));
        if ((quadrants & 4) != 0) boxes.add(new CollisionBox(0.0, stepY0, 0.5, 0.5, stepY1, 1.0));
        if ((quadrants & 8) != 0) boxes.add(new CollisionBox(0.5, stepY0, 0.5, 1.0, stepY1, 1.0));
        return CollisionShape.of(boxes.toArray(new CollisionBox[0]));
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
