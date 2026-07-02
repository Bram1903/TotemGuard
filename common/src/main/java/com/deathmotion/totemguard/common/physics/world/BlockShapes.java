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
import com.github.retrooper.packetevents.protocol.world.states.enums.Attachment;
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
    private static final double SCULK_SENSOR_TOP = 0.5;
    private static final double STONECUTTER_TOP = 0.5625;
    private static final double CAMPFIRE_TOP = 0.4375;
    private static final double CANDLE_TOP = 0.375;
    private static final double TURTLE_EGG_TOP = 0.4375;

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

    private static final CollisionShape FLOWER_POT_SHAPE = CollisionShape.box(0.3125, 0.0, 0.3125, 0.6875, 0.375, 0.6875);
    private static final CollisionShape SCULK_SENSOR_SHAPE = CollisionShape.top(SCULK_SENSOR_TOP);
    private static final CollisionShape STONECUTTER_SHAPE = CollisionShape.top(STONECUTTER_TOP);
    private static final CollisionShape CAMPFIRE_SHAPE = CollisionShape.top(CAMPFIRE_TOP);
    private static final CollisionShape SNIFFER_EGG_SHAPE = CollisionShape.box(INSET, 0.0, 0.125, INSET_MAX, 1.0, 0.875);
    private static final CollisionShape FULL_HEIGHT_POT_SHAPE = CollisionShape.box(INSET, 0.0, INSET, INSET_MAX, 1.0, INSET_MAX);
    private static final CollisionShape TURTLE_EGG_ONE = CollisionShape.box(0.1875, 0.0, 0.1875, 0.75, TURTLE_EGG_TOP, 0.75);
    private static final CollisionShape TURTLE_EGG_MANY = CollisionShape.box(0.0625, 0.0, 0.0625, 0.9375, TURTLE_EGG_TOP, 0.9375);

    private static final CollisionShape CANDLE_ONE = CollisionShape.box(0.4375, 0.0, 0.4375, 0.5625, CANDLE_TOP, 0.5625);
    private static final CollisionShape CANDLE_TWO = CollisionShape.box(0.3125, 0.0, 0.375, 0.6875, CANDLE_TOP, 0.5625);
    private static final CollisionShape CANDLE_THREE = CollisionShape.box(0.3125, 0.0, 0.375, 0.625, CANDLE_TOP, 0.6875);
    private static final CollisionShape CANDLE_FOUR = CollisionShape.box(0.3125, 0.0, 0.3125, 0.6875, CANDLE_TOP, 0.625);

    private static final CollisionShape CANDLE_CAKE_SHAPE = CollisionShape.of(
            new CollisionBox(INSET, 0.0, INSET, INSET_MAX, CAKE_TOP, INSET_MAX),
            new CollisionBox(0.4375, CAKE_TOP, 0.4375, 0.5625, 0.875, 0.5625));

    private static final CollisionShape COMPOSTER_SHAPE = CollisionShape.of(
            new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.125, 1.0),
            new CollisionBox(0.0, 0.125, 0.0, 0.125, 1.0, 1.0),
            new CollisionBox(0.875, 0.125, 0.0, 1.0, 1.0, 1.0),
            new CollisionBox(0.0, 0.125, 0.0, 1.0, 1.0, 0.125),
            new CollisionBox(0.0, 0.125, 0.875, 1.0, 1.0, 1.0));

    private static final CollisionShape LECTERN_SHAPE = CollisionShape.of(
            new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.125, 1.0),
            new CollisionBox(0.25, 0.125, 0.25, 0.75, 0.875, 0.75));

    private static final CollisionBox ANVIL_BASE = new CollisionBox(0.125, 0.0, 0.125, 0.875, 0.25, 0.875);
    private static final CollisionShape ANVIL_X_AXIS = CollisionShape.of(ANVIL_BASE,
            new CollisionBox(0.1875, 0.25, 0.25, 0.8125, 0.3125, 0.75),
            new CollisionBox(0.25, 0.3125, 0.375, 0.75, 0.625, 0.625),
            new CollisionBox(0.0, 0.625, 0.1875, 1.0, 1.0, 0.8125));
    private static final CollisionShape ANVIL_Z_AXIS = CollisionShape.of(ANVIL_BASE,
            new CollisionBox(0.25, 0.25, 0.1875, 0.75, 0.3125, 0.8125),
            new CollisionBox(0.375, 0.3125, 0.25, 0.625, 0.625, 0.75),
            new CollisionBox(0.1875, 0.625, 0.0, 0.8125, 1.0, 1.0));

    private static final double PISTON_ARM_MIN = 0.375;
    private static final double PISTON_ARM_MAX = 0.625;

    private static final CollisionShape MUD_SHAPE = CollisionShape.top(0.875);
    private static final CollisionShape BELL_BODY = CollisionShape.of(
            new CollisionBox(0.3125, 0.375, 0.3125, 0.6875, 0.8125, 0.6875),
            new CollisionBox(0.25, 0.25, 0.25, 0.75, 0.375, 0.75));
    private static final CollisionShape CONDUIT_SHAPE = CollisionShape.box(0.3125, 0.3125, 0.3125, 0.6875, 0.6875, 0.6875);
    private static final CollisionShape AZALEA_SHAPE = CollisionShape.of(
            new CollisionBox(0.0, 0.5, 0.0, 1.0, 1.0, 1.0),
            new CollisionBox(0.375, 0.0, 0.375, 0.625, 0.5, 0.625));
    private static final CollisionShape LANTERN_STANDING = CollisionShape.of(
            new CollisionBox(0.3125, 0.0, 0.3125, 0.6875, 0.4375, 0.6875),
            new CollisionBox(0.375, 0.4375, 0.375, 0.625, 0.5625, 0.625));
    private static final CollisionShape LANTERN_HANGING = CollisionShape.of(
            new CollisionBox(0.3125, 0.0625, 0.3125, 0.6875, 0.5, 0.6875),
            new CollisionBox(0.375, 0.5, 0.375, 0.625, 0.625, 0.625));
    private static final CollisionShape BELL_FLOOR_X = CollisionShape.box(0.25, 0.0, 0.0, 0.75, 1.0, 1.0);
    private static final CollisionShape BELL_FLOOR_Z = CollisionShape.box(0.0, 0.0, 0.25, 1.0, 1.0, 0.75);

    private static final CollisionShape CHAIN_Y = CollisionShape.box(0.40625, 0.0, 0.40625, 0.59375, 1.0, 0.59375);
    private static final CollisionShape CHAIN_X = CollisionShape.box(0.0, 0.40625, 0.40625, 1.0, 0.59375, 0.59375);
    private static final CollisionShape CHAIN_Z = CollisionShape.box(0.40625, 0.40625, 0.0, 0.59375, 0.59375, 1.0);
    private static final CollisionShape ROD_Y = CollisionShape.box(0.375, 0.0, 0.375, 0.625, 1.0, 0.625);
    private static final CollisionShape ROD_X = CollisionShape.box(0.0, 0.375, 0.375, 1.0, 0.625, 0.625);
    private static final CollisionShape ROD_Z = CollisionShape.box(0.375, 0.375, 0.0, 0.625, 0.625, 1.0);

    private static final CollisionShape WALL_SKULL_NORTH = CollisionShape.box(0.25, 0.25, 0.5, 0.75, 0.75, 1.0);
    private static final CollisionShape WALL_SKULL_SOUTH = CollisionShape.box(0.25, 0.25, 0.0, 0.75, 0.75, 0.5);
    private static final CollisionShape WALL_SKULL_WEST = CollisionShape.box(0.5, 0.25, 0.25, 1.0, 0.75, 0.75);
    private static final CollisionShape WALL_SKULL_EAST = CollisionShape.box(0.0, 0.25, 0.25, 0.5, 0.75, 0.75);

    private static final Set<StateType> WALL_SKULLS = Set.of(
            StateTypes.SKELETON_WALL_SKULL, StateTypes.WITHER_SKELETON_WALL_SKULL,
            StateTypes.ZOMBIE_WALL_HEAD, StateTypes.PLAYER_WALL_HEAD,
            StateTypes.CREEPER_WALL_HEAD, StateTypes.DRAGON_WALL_HEAD, StateTypes.PIGLIN_WALL_HEAD);

    private static final CollisionShape HOPPER_BASE = hopper(null);
    private static final CollisionShape HOPPER_DOWN = hopper(new CollisionBox(0.375, 0.0, 0.375, 0.625, 0.25, 0.625));
    private static final CollisionShape HOPPER_NORTH = hopper(new CollisionBox(0.375, 0.25, 0.0, 0.625, 0.5, 0.25));
    private static final CollisionShape HOPPER_SOUTH = hopper(new CollisionBox(0.375, 0.25, 0.75, 0.625, 0.5, 1.0));
    private static final CollisionShape HOPPER_WEST = hopper(new CollisionBox(0.0, 0.25, 0.375, 0.25, 0.5, 0.625));
    private static final CollisionShape HOPPER_EAST = hopper(new CollisionBox(0.75, 0.25, 0.375, 1.0, 0.5, 0.625));

    private static final int[] STAIR_QUADRANTS_BY_STATE = {12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    // Shapes here are modeled approximately (dynamic geometry, attach-face rotations, or the
    // coordinate-seeded bamboo offset). They are excluded from wall judging and their support
    // read is clamped (see supportApproximate). Anything modeled exactly must NOT be here.
    private static final Set<StateType> WALL_UNTRUSTED = Set.of(
            StateTypes.POWDER_SNOW, StateTypes.SCAFFOLDING,
            StateTypes.BAMBOO, StateTypes.CHORUS_PLANT,
            StateTypes.BELL, StateTypes.GRINDSTONE,
            StateTypes.POINTED_DRIPSTONE, StateTypes.BIG_DRIPLEAF,
            StateTypes.AMETHYST_CLUSTER, StateTypes.LARGE_AMETHYST_BUD,
            StateTypes.MEDIUM_AMETHYST_BUD, StateTypes.SMALL_AMETHYST_BUD,
            StateTypes.MOVING_PISTON);

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
        if (type == StateTypes.CAKE) {
            return CollisionShape.box(INSET + state.getBites() * 0.125, 0.0, INSET, INSET_MAX, CAKE_TOP, INSET_MAX);
        }
        if (FLOOR_SKULLS.contains(type)) return CollisionShape.box(SKULL_INSET, 0.0, SKULL_INSET, 1.0 - SKULL_INSET, SKULL_TOP, 1.0 - SKULL_INSET);
        if (type == StateTypes.LADDER) return ladder(state);
        if (CAULDRONS.contains(type)) return cauldron();
        if (type == StateTypes.FLOWER_POT) return FLOWER_POT_SHAPE;
        if (type == StateTypes.TURTLE_EGG) return state.getEggs() > 1 ? TURTLE_EGG_MANY : TURTLE_EGG_ONE;
        if (type == StateTypes.SCULK_SENSOR || type == StateTypes.CALIBRATED_SCULK_SENSOR
                || type == StateTypes.SCULK_SHRIEKER) {
            return SCULK_SENSOR_SHAPE;
        }
        if (type == StateTypes.HOPPER) return hopperByFacing(state);
        if (type == StateTypes.COMPOSTER) return COMPOSTER_SHAPE;
        if (type == StateTypes.STONECUTTER) return STONECUTTER_SHAPE;
        if (type == StateTypes.LECTERN) return LECTERN_SHAPE;
        if (type == StateTypes.DECORATED_POT || type == StateTypes.DRAGON_EGG) return FULL_HEIGHT_POT_SHAPE;
        if (type == StateTypes.SNIFFER_EGG) return SNIFFER_EGG_SHAPE;

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
        if (BlockTags.ANVIL.contains(type)) return anvil(state);
        if (BlockTags.CANDLES.contains(type)) return candles(state);
        if (BlockTags.CANDLE_CAKES.contains(type)) return CANDLE_CAKE_SHAPE;
        if (BlockTags.CAMPFIRES.contains(type)) return CAMPFIRE_SHAPE;
        if (type == StateTypes.HEAVY_CORE) return CollisionShape.box(0.25, 0.0, 0.25, 0.75, 0.5, 0.75);
        if (GOLEM_STATUES.contains(type)) return CollisionShape.box(0.1875, 0.0, 0.1875, 0.8125, 0.875, 0.8125);

        if (type == StateTypes.MUD) return MUD_SHAPE;
        if (type == StateTypes.PISTON || type == StateTypes.STICKY_PISTON) return pistonBase(state);
        if (type == StateTypes.PISTON_HEAD) return pistonHead(state);
        if (type == StateTypes.CONDUIT) return CONDUIT_SHAPE;
        if (type == StateTypes.AZALEA || type == StateTypes.FLOWERING_AZALEA) return AZALEA_SHAPE;
        if (type == StateTypes.LANTERN || type == StateTypes.SOUL_LANTERN) {
            return state.isHanging() ? LANTERN_HANGING : LANTERN_STANDING;
        }
        if (type == StateTypes.CHAIN) return chain(state);
        if (type == StateTypes.END_ROD || type == StateTypes.LIGHTNING_ROD) return rod(state);
        if (WALL_SKULLS.contains(type)) return wallSkull(state);
        if (type == StateTypes.BELL) return bell(state);

        return type.isBlocking() && type.isSolid() ? CollisionShape.FULL : CollisionShape.EMPTY;
    }

    public static boolean wallTrusted(StateType type) {
        return !WALL_UNTRUSTED.contains(type);
    }

    public static boolean supportApproximate(StateType type) {
        return WALL_UNTRUSTED.contains(type);
    }

    public static boolean suffocatingOverride(StateType type) {
        return type == StateTypes.SOUL_SAND || type == StateTypes.MUD
                || type == StateTypes.FARMLAND
                || type == StateTypes.DIRT_PATH || type == StateTypes.GRASS_PATH;
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

    private static CollisionShape hopper(CollisionBox spout) {
        List<CollisionBox> boxes = new ArrayList<>(7);
        boxes.add(new CollisionBox(0.25, 0.25, 0.25, 0.75, 0.625, 0.75));
        boxes.add(new CollisionBox(0.0, 0.625, 0.0, 1.0, 0.6875, 1.0));
        boxes.add(new CollisionBox(0.0, 0.6875, 0.0, 0.125, 1.0, 1.0));
        boxes.add(new CollisionBox(0.875, 0.6875, 0.0, 1.0, 1.0, 1.0));
        boxes.add(new CollisionBox(0.125, 0.6875, 0.0, 0.875, 1.0, 0.125));
        boxes.add(new CollisionBox(0.125, 0.6875, 0.875, 0.875, 1.0, 1.0));
        if (spout != null) boxes.add(spout);
        return CollisionShape.of(boxes.toArray(new CollisionBox[0]));
    }

    private static CollisionShape hopperByFacing(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case DOWN -> HOPPER_DOWN;
            case NORTH -> HOPPER_NORTH;
            case SOUTH -> HOPPER_SOUTH;
            case WEST -> HOPPER_WEST;
            case EAST -> HOPPER_EAST;
            default -> HOPPER_BASE;
        };
    }

    private static CollisionShape anvil(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case WEST, EAST -> ANVIL_X_AXIS;
            default -> ANVIL_Z_AXIS;
        };
    }

    private static CollisionShape candles(WrappedBlockState state) {
        return switch (state.getCandles()) {
            case 2 -> CANDLE_TWO;
            case 3 -> CANDLE_THREE;
            case 4 -> CANDLE_FOUR;
            default -> CANDLE_ONE;
        };
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

    private static CollisionShape pistonBase(WrappedBlockState state) {
        if (!state.isExtended()) return CollisionShape.FULL;
        return switch (state.getFacing()) {
            case UP -> CollisionShape.box(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);
            case DOWN -> CollisionShape.box(0.0, 0.25, 0.0, 1.0, 1.0, 1.0);
            case NORTH -> CollisionShape.box(0.0, 0.0, 0.25, 1.0, 1.0, 1.0);
            case SOUTH -> CollisionShape.box(0.0, 0.0, 0.0, 1.0, 1.0, 0.75);
            case WEST -> CollisionShape.box(0.25, 0.0, 0.0, 1.0, 1.0, 1.0);
            default -> CollisionShape.box(0.0, 0.0, 0.0, 0.75, 1.0, 1.0);
        };
    }

    private static CollisionShape pistonHead(WrappedBlockState state) {
        double a0 = PISTON_ARM_MIN, a1 = PISTON_ARM_MAX;
        return switch (state.getFacing()) {
            case UP -> CollisionShape.of(
                    new CollisionBox(0.0, 0.75, 0.0, 1.0, 1.0, 1.0),
                    new CollisionBox(a0, 0.0, a0, a1, 0.75, a1));
            case DOWN -> CollisionShape.of(
                    new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.25, 1.0),
                    new CollisionBox(a0, 0.25, a0, a1, 1.0, a1));
            case NORTH -> CollisionShape.of(
                    new CollisionBox(0.0, 0.0, 0.0, 1.0, 1.0, 0.25),
                    new CollisionBox(a0, a0, 0.25, a1, a1, 1.0));
            case SOUTH -> CollisionShape.of(
                    new CollisionBox(0.0, 0.0, 0.75, 1.0, 1.0, 1.0),
                    new CollisionBox(a0, a0, 0.0, a1, a1, 0.75));
            case WEST -> CollisionShape.of(
                    new CollisionBox(0.0, 0.0, 0.0, 0.25, 1.0, 1.0),
                    new CollisionBox(0.25, a0, a0, 1.0, a1, a1));
            default -> CollisionShape.of(
                    new CollisionBox(0.75, 0.0, 0.0, 1.0, 1.0, 1.0),
                    new CollisionBox(0.0, a0, a0, 0.75, a1, a1));
        };
    }

    private static CollisionShape chain(WrappedBlockState state) {
        return switch (state.getAxis()) {
            case X -> CHAIN_X;
            case Z -> CHAIN_Z;
            default -> CHAIN_Y;
        };
    }

    private static CollisionShape rod(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case NORTH, SOUTH -> ROD_Z;
            case WEST, EAST -> ROD_X;
            default -> ROD_Y;
        };
    }

    private static CollisionShape wallSkull(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case SOUTH -> WALL_SKULL_SOUTH;
            case WEST -> WALL_SKULL_WEST;
            case EAST -> WALL_SKULL_EAST;
            default -> WALL_SKULL_NORTH;
        };
    }

    private static CollisionShape bell(WrappedBlockState state) {
        if (state.getAttachment() == Attachment.FLOOR) {
            return switch (state.getFacing()) {
                case WEST, EAST -> BELL_FLOOR_X;
                default -> BELL_FLOOR_Z;
            };
        }
        return BELL_BODY;
    }

    private static CollisionShape scaffolding(int cellY, CollisionContext ctx) {
        boolean above = ctx.feetY() > cellY + SCAFFOLD_STABLE_TOP - ABOVE_EPS;
        if (above && !ctx.descending()) return CollisionShape.top(SCAFFOLD_STABLE_TOP);
        return CollisionShape.EMPTY;
    }
}
