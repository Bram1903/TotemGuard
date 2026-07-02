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
import com.github.retrooper.packetevents.protocol.world.states.enums.Face;
import com.github.retrooper.packetevents.protocol.world.states.enums.Half;
import com.github.retrooper.packetevents.protocol.world.states.enums.Hinge;
import com.github.retrooper.packetevents.protocol.world.states.enums.North;
import com.github.retrooper.packetevents.protocol.world.states.enums.Part;
import com.github.retrooper.packetevents.protocol.world.states.enums.South;
import com.github.retrooper.packetevents.protocol.world.states.enums.Thickness;
import com.github.retrooper.packetevents.protocol.world.states.enums.Tilt;
import com.github.retrooper.packetevents.protocol.world.states.enums.Type;
import com.github.retrooper.packetevents.protocol.world.states.enums.VerticalDirection;
import com.github.retrooper.packetevents.protocol.world.states.enums.West;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BlockShapes {

    @FunctionalInterface
    private interface ShapeResolver {
        CollisionShape resolve(WrappedBlockState state, int x, int y, int z, CollisionContext ctx);
    }

    private static final double PX = 1.0 / 16.0;

    private static CollisionBox px(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new CollisionBox(x0 * PX, y0 * PX, z0 * PX, x1 * PX, y1 * PX, z1 * PX);
    }

    private static CollisionShape pxShape(double x0, double y0, double z0, double x1, double y1, double z1) {
        return CollisionShape.of(px(x0, y0, z0, x1, y1, z1));
    }

    private static final CollisionShape SOUL_SAND_SHAPE = CollisionShape.top(0.875);
    private static final CollisionShape MUD_SHAPE = CollisionShape.top(0.875);
    private static final CollisionShape PATH_SHAPE = CollisionShape.top(0.9375);
    private static final CollisionShape ENCHANTING_TABLE_SHAPE = CollisionShape.top(0.75);
    private static final CollisionShape DAYLIGHT_DETECTOR_SHAPE = CollisionShape.top(0.375);
    private static final CollisionShape DIODE_SHAPE = CollisionShape.top(0.125);
    private static final CollisionShape CARPET_SHAPE = CollisionShape.top(0.0625);
    private static final CollisionShape STONECUTTER_SHAPE = CollisionShape.top(0.5625);
    private static final CollisionShape CAMPFIRE_SHAPE = CollisionShape.top(0.4375);
    private static final CollisionShape SCULK_SENSOR_SHAPE = CollisionShape.top(0.5);
    private static final CollisionShape SLAB_BOTTOM = CollisionShape.top(0.5);
    private static final CollisionShape SLAB_TOP = CollisionShape.box(0.0, 0.5, 0.0, 1.0, 1.0, 1.0);

    private static final CollisionShape HONEY_CACTUS_SHAPE = pxShape(1, 0, 1, 15, 15, 15);
    private static final CollisionShape LILY_PAD_SHAPE = pxShape(1, 0, 1, 15, 1.5, 15);
    private static final CollisionShape CHEST_SINGLE = pxShape(1, 0, 1, 15, 14, 15);
    private static final CollisionShape CHEST_NORTH = pxShape(1, 0, 0, 15, 14, 15);
    private static final CollisionShape CHEST_SOUTH = pxShape(1, 0, 1, 15, 14, 16);
    private static final CollisionShape CHEST_WEST = pxShape(0, 0, 1, 15, 14, 15);
    private static final CollisionShape CHEST_EAST = pxShape(1, 0, 1, 16, 14, 15);
    private static final CollisionShape FULL_HEIGHT_POT_SHAPE = pxShape(1, 0, 1, 15, 16, 15);
    private static final CollisionShape FLOWER_POT_SHAPE = pxShape(5, 0, 5, 11, 6, 11);
    private static final CollisionShape CONDUIT_SHAPE = pxShape(5, 5, 5, 11, 11, 11);
    private static final CollisionShape HEAVY_CORE_SHAPE = pxShape(4, 0, 4, 12, 8, 12);
    private static final CollisionShape GOLEM_STATUE_SHAPE = pxShape(3, 0, 3, 13, 14, 13);
    private static final CollisionShape DRIED_GHAST_SHAPE = pxShape(3, 0, 3, 13, 10, 13);
    private static final CollisionShape SNIFFER_EGG_SHAPE = pxShape(1, 0, 2, 15, 16, 14);
    private static final CollisionShape TURTLE_EGG_ONE = pxShape(3, 0, 3, 12, 7, 12);
    private static final CollisionShape TURTLE_EGG_MANY = pxShape(1, 0, 1, 15, 7, 15);
    private static final CollisionShape SKULL_SHAPE = pxShape(4, 0, 4, 12, 8, 12);
    private static final CollisionShape PIGLIN_SKULL_SHAPE = pxShape(3, 0, 3, 13, 8, 13);
    private static final CollisionShape END_PORTAL_FRAME_SHAPE = CollisionShape.top(0.8125);
    private static final CollisionShape END_PORTAL_FRAME_EYE = CollisionShape.of(
            px(0, 0, 0, 16, 13, 16), px(4, 13, 4, 12, 16, 12));

    private static final CollisionShape SEA_PICKLE_ONE = pxShape(6, 0, 6, 10, 6, 10);
    private static final CollisionShape SEA_PICKLE_TWO = pxShape(3, 0, 3, 13, 6, 13);
    private static final CollisionShape SEA_PICKLE_THREE = pxShape(2, 0, 2, 14, 6, 14);
    private static final CollisionShape SEA_PICKLE_FOUR = pxShape(2, 0, 2, 14, 7, 14);

    private static final CollisionShape CANDLE_ONE = pxShape(7, 0, 7, 9, 6, 9);
    private static final CollisionShape CANDLE_TWO = pxShape(5, 0, 6, 11, 6, 9);
    private static final CollisionShape CANDLE_THREE = pxShape(5, 0, 6, 10, 6, 11);
    private static final CollisionShape CANDLE_FOUR = pxShape(5, 0, 5, 11, 6, 10);

    private static final CollisionShape CANDLE_CAKE_SHAPE = CollisionShape.of(
            px(1, 0, 1, 15, 8, 15), px(7, 8, 7, 9, 14, 9));

    private static final CollisionShape BREWING_STAND_SHAPE = CollisionShape.of(
            px(1, 0, 1, 15, 2, 15), px(7, 2, 7, 9, 14, 9));

    private static final CollisionShape LECTERN_SHAPE = CollisionShape.of(
            px(0, 0, 0, 16, 2, 16), px(4, 2, 4, 12, 14, 12));

    private static final CollisionShape PITCHER_CROP_BULB = pxShape(5, -1, 5, 11, 3, 11);
    private static final CollisionShape PITCHER_CROP_GROWN = pxShape(3, -1, 3, 13, 5, 13);

    private static final CollisionShape BIG_DRIPLEAF_FLAT = pxShape(0, 11, 0, 16, 15, 16);
    private static final CollisionShape BIG_DRIPLEAF_TILTED = pxShape(0, 11, 0, 16, 13, 16);

    private static final CollisionShape AZALEA_SHAPE = CollisionShape.of(
            px(0, 8, 0, 16, 16, 16), px(6, 0, 6, 10, 8, 10));

    private static final CollisionShape LANTERN_STANDING = CollisionShape.of(
            px(5, 0, 5, 11, 7, 11), px(6, 7, 6, 10, 9, 10));
    private static final CollisionShape LANTERN_HANGING = CollisionShape.of(
            px(5, 1, 5, 11, 8, 11), px(6, 8, 6, 10, 10, 10));

    private static final CollisionShape CHAIN_Y = pxShape(6.5, 0, 6.5, 9.5, 16, 9.5);
    private static final CollisionShape CHAIN_X = pxShape(0, 6.5, 6.5, 16, 9.5, 9.5);
    private static final CollisionShape CHAIN_Z = pxShape(6.5, 6.5, 0, 9.5, 9.5, 16);
    private static final CollisionShape ROD_Y = pxShape(6, 0, 6, 10, 16, 10);
    private static final CollisionShape ROD_X = pxShape(0, 6, 6, 16, 10, 10);
    private static final CollisionShape ROD_Z = pxShape(6, 6, 0, 10, 10, 16);

    private static final CollisionShape WALL_SKULL_NORTH = pxShape(4, 4, 8, 12, 12, 16);
    private static final CollisionShape WALL_SKULL_SOUTH = pxShape(4, 4, 0, 12, 12, 8);
    private static final CollisionShape WALL_SKULL_WEST = pxShape(8, 4, 4, 16, 12, 12);
    private static final CollisionShape WALL_SKULL_EAST = pxShape(0, 4, 4, 8, 12, 12);
    private static final CollisionShape PIGLIN_WALL_SKULL_NORTH = pxShape(3, 4, 8, 13, 12, 16);
    private static final CollisionShape PIGLIN_WALL_SKULL_SOUTH = pxShape(3, 4, 0, 13, 12, 8);
    private static final CollisionShape PIGLIN_WALL_SKULL_WEST = pxShape(8, 4, 3, 16, 12, 13);
    private static final CollisionShape PIGLIN_WALL_SKULL_EAST = pxShape(0, 4, 3, 8, 12, 13);

    private static final CollisionShape HANGING_SIGN_PLANK_Z = pxShape(0, 14, 6, 16, 16, 10);
    private static final CollisionShape HANGING_SIGN_PLANK_X = pxShape(6, 14, 0, 10, 16, 16);

    private static final CollisionShape DOOR_NORTH = pxShape(0, 0, 13, 16, 16, 16);
    private static final CollisionShape DOOR_SOUTH = pxShape(0, 0, 0, 16, 16, 3);
    private static final CollisionShape DOOR_WEST = pxShape(13, 0, 0, 16, 16, 16);
    private static final CollisionShape DOOR_EAST = pxShape(0, 0, 0, 3, 16, 16);

    private static final CollisionShape TRAPDOOR_BOTTOM = CollisionShape.top(0.1875);
    private static final CollisionShape TRAPDOOR_TOP = pxShape(0, 13, 0, 16, 16, 16);

    private static final CollisionShape BED_NORTH = CollisionShape.of(
            px(0, 3, 0, 16, 9, 16), px(0, 0, 0, 3, 3, 3), px(13, 0, 0, 16, 3, 3));
    private static final CollisionShape BED_SOUTH = CollisionShape.of(
            px(0, 3, 0, 16, 9, 16), px(0, 0, 13, 3, 3, 16), px(13, 0, 13, 16, 3, 16));
    private static final CollisionShape BED_WEST = CollisionShape.of(
            px(0, 3, 0, 16, 9, 16), px(0, 0, 0, 3, 3, 3), px(0, 0, 13, 3, 3, 16));
    private static final CollisionShape BED_EAST = CollisionShape.of(
            px(0, 3, 0, 16, 9, 16), px(13, 0, 0, 16, 3, 3), px(13, 0, 13, 16, 3, 16));

    private static final CollisionShape CAULDRON_SHAPE = CollisionShape.of(
            px(0, 3, 0, 16, 4, 16),
            px(0, 4, 0, 2, 16, 16), px(14, 4, 0, 16, 16, 16),
            px(2, 4, 0, 14, 16, 2), px(2, 4, 14, 14, 16, 16),
            px(0, 0, 0, 2, 3, 4), px(2, 0, 0, 4, 3, 2),
            px(14, 0, 0, 16, 3, 4), px(12, 0, 0, 14, 3, 2),
            px(0, 0, 12, 2, 3, 16), px(2, 0, 14, 4, 3, 16),
            px(14, 0, 12, 16, 3, 16), px(12, 0, 14, 14, 3, 16));

    private static final CollisionShape COMPOSTER_SHAPE = CollisionShape.of(
            px(0, 0, 0, 16, 2, 16),
            px(0, 2, 0, 2, 16, 16), px(14, 2, 0, 16, 16, 16),
            px(2, 2, 0, 14, 16, 2), px(2, 2, 14, 14, 16, 16));

    private static final CollisionShape HOPPER_BODY = CollisionShape.of(
            px(0, 10, 0, 2, 16, 16), px(14, 10, 0, 16, 16, 16),
            px(2, 10, 0, 14, 16, 2), px(2, 10, 14, 14, 16, 16),
            px(2, 10, 2, 14, 11, 14), px(4, 4, 4, 12, 10, 12));
    private static final CollisionShape HOPPER_DOWN = hopper(px(6, 0, 6, 10, 4, 10));
    private static final CollisionShape HOPPER_NORTH = hopper(px(6, 4, 0, 10, 8, 4));
    private static final CollisionShape HOPPER_SOUTH = hopper(px(6, 4, 12, 10, 8, 16));
    private static final CollisionShape HOPPER_WEST = hopper(px(0, 4, 6, 4, 8, 10));
    private static final CollisionShape HOPPER_EAST = hopper(px(12, 4, 6, 16, 8, 10));

    private static final CollisionBox ANVIL_BASE = px(2, 0, 2, 14, 4, 14);
    private static final CollisionShape ANVIL_X = CollisionShape.of(ANVIL_BASE,
            px(3, 4, 4, 13, 5, 12), px(4, 5, 6, 12, 10, 10), px(0, 10, 3, 16, 16, 13));
    private static final CollisionShape ANVIL_Z = CollisionShape.of(ANVIL_BASE,
            px(4, 4, 3, 12, 5, 13), px(6, 5, 4, 10, 10, 12), px(3, 10, 0, 13, 16, 16));

    private static final CollisionBox BELL_TOP = px(5, 6, 5, 11, 13, 11);
    private static final CollisionBox BELL_BOTTOM = px(4, 4, 4, 12, 6, 12);
    private static final CollisionShape BELL_FLOOR_Z = pxShape(0, 0, 4, 16, 16, 12);
    private static final CollisionShape BELL_FLOOR_X = pxShape(4, 0, 0, 12, 16, 16);
    private static final CollisionShape BELL_CEILING = CollisionShape.of(BELL_TOP, BELL_BOTTOM, px(7, 13, 7, 9, 16, 9));
    private static final CollisionShape BELL_DOUBLE_Z = CollisionShape.of(BELL_TOP, BELL_BOTTOM, px(7, 13, 0, 9, 15, 16));
    private static final CollisionShape BELL_DOUBLE_X = CollisionShape.of(BELL_TOP, BELL_BOTTOM, px(0, 13, 7, 16, 15, 9));
    private static final CollisionShape BELL_WALL_NORTH = CollisionShape.of(BELL_TOP, BELL_BOTTOM, px(7, 13, 0, 9, 15, 13));
    private static final CollisionShape BELL_WALL_SOUTH = CollisionShape.of(BELL_TOP, BELL_BOTTOM, px(7, 13, 3, 9, 15, 16));
    private static final CollisionShape BELL_WALL_WEST = CollisionShape.of(BELL_TOP, BELL_BOTTOM, px(0, 13, 7, 13, 15, 9));
    private static final CollisionShape BELL_WALL_EAST = CollisionShape.of(BELL_TOP, BELL_BOTTOM, px(3, 13, 7, 16, 15, 9));

    private static final CollisionShape GRIND_WALL_NORTH = CollisionShape.of(px(4, 2, 0, 12, 14, 12),
            px(2, 6, 7, 4, 10, 16), px(12, 6, 7, 14, 10, 16), px(2, 5, 3, 4, 11, 9), px(12, 5, 3, 14, 11, 9));
    private static final CollisionShape GRIND_WALL_SOUTH = CollisionShape.of(px(4, 2, 4, 12, 14, 16),
            px(2, 6, 0, 4, 10, 9), px(12, 6, 0, 14, 10, 9), px(2, 5, 7, 4, 11, 13), px(12, 5, 7, 14, 11, 13));
    private static final CollisionShape GRIND_WALL_EAST = CollisionShape.of(px(4, 2, 4, 16, 14, 12),
            px(0, 6, 2, 9, 10, 4), px(0, 6, 12, 9, 10, 14), px(7, 5, 2, 13, 11, 4), px(7, 5, 12, 13, 11, 14));
    private static final CollisionShape GRIND_WALL_WEST = CollisionShape.of(px(0, 2, 4, 12, 14, 12),
            px(7, 6, 2, 16, 10, 4), px(7, 6, 12, 16, 10, 14), px(3, 5, 2, 9, 11, 4), px(3, 5, 12, 9, 11, 14));
    private static final CollisionShape GRIND_FLOOR_Z = CollisionShape.of(px(4, 4, 2, 12, 16, 14),
            px(2, 0, 6, 4, 9, 10), px(12, 0, 6, 14, 9, 10), px(2, 7, 5, 4, 13, 11), px(12, 7, 5, 14, 13, 11));
    private static final CollisionShape GRIND_FLOOR_X = CollisionShape.of(px(2, 4, 4, 14, 16, 12),
            px(6, 0, 2, 10, 9, 4), px(6, 0, 12, 10, 9, 14), px(5, 7, 2, 11, 13, 4), px(5, 7, 12, 11, 13, 14));
    private static final CollisionShape GRIND_CEILING_Z = CollisionShape.of(px(4, 0, 2, 12, 12, 14),
            px(2, 7, 6, 4, 16, 10), px(12, 7, 6, 14, 16, 10), px(2, 3, 5, 4, 9, 11), px(12, 3, 5, 14, 9, 11));
    private static final CollisionShape GRIND_CEILING_X = CollisionShape.of(px(2, 0, 4, 14, 12, 12),
            px(6, 7, 2, 10, 16, 4), px(6, 7, 12, 10, 16, 14), px(5, 3, 2, 11, 9, 4), px(5, 3, 12, 11, 9, 14));

    private static final CollisionShape PISTON_UP = CollisionShape.top(0.75);
    private static final CollisionShape PISTON_DOWN = pxShape(0, 4, 0, 16, 16, 16);
    private static final CollisionShape PISTON_NORTH = pxShape(0, 0, 4, 16, 16, 16);
    private static final CollisionShape PISTON_SOUTH = pxShape(0, 0, 0, 16, 16, 12);
    private static final CollisionShape PISTON_WEST = pxShape(4, 0, 0, 16, 16, 16);
    private static final CollisionShape PISTON_EAST = pxShape(0, 0, 0, 12, 16, 16);

    private static final CollisionShape HEAD_UP = CollisionShape.of(px(0, 12, 0, 16, 16, 16), px(6, -4, 6, 10, 12, 10));
    private static final CollisionShape HEAD_UP_SHORT = CollisionShape.of(px(0, 12, 0, 16, 16, 16), px(6, 0, 6, 10, 12, 10));
    private static final CollisionShape HEAD_DOWN = CollisionShape.of(px(0, 0, 0, 16, 4, 16), px(6, 4, 6, 10, 20, 10));
    private static final CollisionShape HEAD_DOWN_SHORT = CollisionShape.of(px(0, 0, 0, 16, 4, 16), px(6, 4, 6, 10, 16, 10));
    private static final CollisionShape HEAD_NORTH = CollisionShape.of(px(0, 0, 0, 16, 16, 4), px(6, 6, 4, 10, 10, 20));
    private static final CollisionShape HEAD_NORTH_SHORT = CollisionShape.of(px(0, 0, 0, 16, 16, 4), px(6, 6, 4, 10, 10, 16));
    private static final CollisionShape HEAD_SOUTH = CollisionShape.of(px(0, 0, 12, 16, 16, 16), px(6, 6, -4, 10, 10, 12));
    private static final CollisionShape HEAD_SOUTH_SHORT = CollisionShape.of(px(0, 0, 12, 16, 16, 16), px(6, 6, 0, 10, 10, 12));
    private static final CollisionShape HEAD_WEST = CollisionShape.of(px(0, 0, 0, 4, 16, 16), px(4, 6, 6, 20, 10, 10));
    private static final CollisionShape HEAD_WEST_SHORT = CollisionShape.of(px(0, 0, 0, 4, 16, 16), px(4, 6, 6, 16, 10, 10));
    private static final CollisionShape HEAD_EAST = CollisionShape.of(px(12, 0, 0, 16, 16, 16), px(-4, 6, 6, 12, 10, 10));
    private static final CollisionShape HEAD_EAST_SHORT = CollisionShape.of(px(12, 0, 0, 16, 16, 16), px(0, 6, 6, 12, 10, 10));

    private static final CollisionBox PIPE_CORE = px(3, 3, 3, 13, 13, 13);
    private static final CollisionBox PIPE_NORTH = px(3, 3, 0, 13, 13, 8);
    private static final CollisionBox PIPE_SOUTH = px(3, 3, 8, 13, 13, 16);
    private static final CollisionBox PIPE_WEST = px(0, 3, 3, 8, 13, 13);
    private static final CollisionBox PIPE_EAST = px(8, 3, 3, 16, 13, 13);
    private static final CollisionBox PIPE_DOWN = px(3, 0, 3, 13, 8, 13);
    private static final CollisionBox PIPE_UP = px(3, 8, 3, 13, 16, 13);

    private static final CollisionShape SCAFFOLD_STABLE = CollisionShape.of(
            px(0, 14, 0, 16, 16, 16),
            px(0, 0, 0, 2, 16, 2), px(14, 0, 0, 16, 16, 2),
            px(0, 0, 14, 2, 16, 16), px(14, 0, 14, 16, 16, 16));
    private static final CollisionShape SCAFFOLD_BOTTOM = CollisionShape.top(0.125);
    private static final double ABOVE_EPS = 1.0e-5;

    private static final CollisionBox SPELEO_TIP_MERGE = px(5, 0, 5, 11, 16, 11);
    private static final CollisionBox SPELEO_TIP_UP = px(5, 0, 5, 11, 11, 11);
    private static final CollisionBox SPELEO_TIP_DOWN = px(5, 5, 5, 11, 16, 11);
    private static final CollisionBox SPELEO_FRUSTUM = px(4, 0, 4, 12, 16, 12);
    private static final CollisionBox SPELEO_MIDDLE = px(3, 0, 3, 13, 16, 13);
    private static final CollisionBox SPELEO_BASE = px(2, 0, 2, 14, 16, 14);
    private static final float SPELEO_MAX_OFFSET = 0.125f;

    private static final CollisionBox BAMBOO_BOX = px(6.5, 0, 6.5, 9.5, 16, 9.5);
    private static final float BAMBOO_MAX_OFFSET = 0.25f;

    private static final CollisionShape SHELF_NORTH = CollisionShape.of(
            px(0, 12, 11, 16, 16, 13), px(0, 0, 13, 16, 16, 16), px(0, 0, 11, 16, 4, 13));
    private static final CollisionShape SHELF_EAST = CollisionShape.of(
            px(3, 12, 0, 5, 16, 16), px(0, 0, 0, 3, 16, 16), px(3, 0, 0, 5, 4, 16));
    private static final CollisionShape SHELF_SOUTH = CollisionShape.of(
            px(0, 12, 3, 16, 16, 5), px(0, 0, 0, 16, 16, 3), px(0, 0, 3, 16, 4, 5));
    private static final CollisionShape SHELF_WEST = CollisionShape.of(
            px(11, 12, 0, 13, 16, 16), px(13, 0, 0, 16, 16, 16), px(11, 0, 0, 13, 4, 16));

    private static final CollisionShape[][] COCOA = {
            {pxShape(6, 7, 1, 10, 12, 5), pxShape(5, 5, 1, 11, 12, 7), pxShape(4, 3, 1, 12, 12, 9)},
            {pxShape(11, 7, 6, 15, 12, 10), pxShape(9, 5, 5, 15, 12, 11), pxShape(7, 3, 4, 15, 12, 12)},
            {pxShape(6, 7, 11, 10, 12, 15), pxShape(5, 5, 9, 11, 12, 15), pxShape(4, 3, 7, 12, 12, 15)},
            {pxShape(1, 7, 6, 5, 12, 10), pxShape(1, 5, 5, 7, 12, 11), pxShape(1, 3, 4, 9, 12, 12)}};

    private static final double FENCE_TOP = 1.5;
    private static final double FENCE_ARM_HALF = 0.125;
    private static final double PANE_ARM_HALF = 0.0625;
    private static final double WALL_POST_HALF = 0.25;
    private static final double WALL_ARM_HALF = 0.1875;

    private static final int[] STAIR_QUADRANTS_BY_STATE = {12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    private static final Set<StateType> FLOOR_SKULLS = Set.of(
            StateTypes.SKELETON_SKULL, StateTypes.WITHER_SKELETON_SKULL, StateTypes.ZOMBIE_HEAD,
            StateTypes.PLAYER_HEAD, StateTypes.CREEPER_HEAD, StateTypes.DRAGON_HEAD);

    private static final Set<StateType> WALL_SKULLS = Set.of(
            StateTypes.SKELETON_WALL_SKULL, StateTypes.WITHER_SKELETON_WALL_SKULL,
            StateTypes.ZOMBIE_WALL_HEAD, StateTypes.PLAYER_WALL_HEAD,
            StateTypes.CREEPER_WALL_HEAD, StateTypes.DRAGON_WALL_HEAD);

    private static final Map<StateType, ShapeResolver> RESOLVERS = new HashMap<>(1024);

    private static final Set<StateType> APPROXIMATE = new HashSet<>();

    private static final Set<StateType> SUFFOCATING_ALWAYS = new HashSet<>();

    private static final Set<StateType> SUFFOCATING_NEVER = new HashSet<>();

    static {
        registerAll();
    }

    private BlockShapes() {
    }

    public static CollisionShape shapeOf(WrappedBlockState state, int x, int y, int z, CollisionContext ctx) {
        StateType type = state.getType();
        if (type == StateTypes.AIR || MovementBlocks.isFluidType(type)) return CollisionShape.EMPTY;
        ShapeResolver resolver = RESOLVERS.get(type);
        if (resolver != null) return resolver.resolve(state, x, y, z, ctx);
        return type.isBlocking() && type.isSolid() ? CollisionShape.FULL : CollisionShape.EMPTY;
    }

    public static boolean wallTrusted(StateType type) {
        return !APPROXIMATE.contains(type);
    }

    public static boolean supportApproximate(StateType type) {
        return APPROXIMATE.contains(type);
    }

    public static boolean suffocatingOverride(StateType type) {
        return SUFFOCATING_ALWAYS.contains(type);
    }

    public static boolean suffocatingNever(StateType type) {
        return SUFFOCATING_NEVER.contains(type);
    }

    private static void registerAll() {
        tag(BlockTags.SLABS, (state, x, y, z, ctx) -> switch (state.getTypeData()) {
            case TOP -> SLAB_TOP;
            case DOUBLE -> CollisionShape.FULL;
            default -> SLAB_BOTTOM;
        });
        tag(BlockTags.STAIRS, state(BlockShapes::stairs));
        tag(BlockTags.FENCES, state(BlockShapes::fence));
        tag(BlockTags.FENCE_GATES, state(BlockShapes::fenceGate));
        tag(BlockTags.WALLS, state(BlockShapes::wall));
        tag(BlockTags.GLASS_PANES, state(BlockShapes::pane));
        tag(BlockTags.BARS, state(BlockShapes::pane));
        tag(BlockTags.DOORS, state(BlockShapes::door));
        tag(BlockTags.TRAPDOORS, state(BlockShapes::trapdoor));
        tag(BlockTags.BEDS, state(BlockShapes::bed));
        tag(BlockTags.CANDLES, state(BlockShapes::candles));
        tag(BlockTags.CANDLE_CAKES, fixed(CANDLE_CAKE_SHAPE));
        tag(BlockTags.CAMPFIRES, fixed(CAMPFIRE_SHAPE));
        tag(BlockTags.ANVIL, state(BlockShapes::anvil));
        tag(BlockTags.WOOL_CARPETS, fixed(CARPET_SHAPE));
        tag(BlockTags.CAULDRONS, fixed(CAULDRON_SHAPE));
        tag(BlockTags.FLOWER_POTS, fixed(FLOWER_POT_SHAPE));
        tag(BlockTags.LANTERNS, state(s -> s.isHanging() ? LANTERN_HANGING : LANTERN_STANDING));
        tag(BlockTags.CHAINS, state(BlockShapes::chain));
        tag(BlockTags.LIGHTNING_RODS, state(BlockShapes::rod));
        tag(BlockTags.WOODEN_SHELVES, state(BlockShapes::shelf));
        tag(BlockTags.WALL_HANGING_SIGNS, state(BlockShapes::wallHangingSign));
        tag(BlockTags.COPPER_GOLEM_STATUES, fixed(GOLEM_STATUE_SHAPE));
        tag(BlockTags.SPELEOTHEMS, BlockShapes::speleothem);

        tag(BlockTags.SHULKER_BOXES, fixed(CollisionShape.FULL));
        BlockTags.SHULKER_BOXES.getStates().forEach(APPROXIMATE::add);

        fixed(SOUL_SAND_SHAPE, StateTypes.SOUL_SAND);
        fixed(MUD_SHAPE, StateTypes.MUD);
        fixed(PATH_SHAPE, StateTypes.FARMLAND, StateTypes.DIRT_PATH, StateTypes.GRASS_PATH);
        fixed(ENCHANTING_TABLE_SHAPE, StateTypes.ENCHANTING_TABLE);
        fixed(DAYLIGHT_DETECTOR_SHAPE, StateTypes.DAYLIGHT_DETECTOR);
        fixed(DIODE_SHAPE, StateTypes.REPEATER, StateTypes.COMPARATOR);
        fixed(STONECUTTER_SHAPE, StateTypes.STONECUTTER);
        fixed(SCULK_SENSOR_SHAPE, StateTypes.SCULK_SENSOR, StateTypes.CALIBRATED_SCULK_SENSOR, StateTypes.SCULK_SHRIEKER);
        fixed(HONEY_CACTUS_SHAPE, StateTypes.HONEY_BLOCK, StateTypes.CACTUS);
        fixed(LILY_PAD_SHAPE, StateTypes.LILY_PAD);
        fixed(CARPET_SHAPE, StateTypes.MOSS_CARPET);
        fixed(FULL_HEIGHT_POT_SHAPE, StateTypes.DECORATED_POT, StateTypes.DRAGON_EGG);
        fixed(CONDUIT_SHAPE, StateTypes.CONDUIT);
        fixed(HEAVY_CORE_SHAPE, StateTypes.HEAVY_CORE);
        fixed(DRIED_GHAST_SHAPE, StateTypes.DRIED_GHAST);
        fixed(SNIFFER_EGG_SHAPE, StateTypes.SNIFFER_EGG);
        fixed(AZALEA_SHAPE, StateTypes.AZALEA, StateTypes.FLOWERING_AZALEA);
        fixed(COMPOSTER_SHAPE, StateTypes.COMPOSTER);
        fixed(LECTERN_SHAPE, StateTypes.LECTERN);
        fixed(BREWING_STAND_SHAPE, StateTypes.BREWING_STAND);
        fixed(SKULL_SHAPE, FLOOR_SKULLS.toArray(new StateType[0]));
        fixed(PIGLIN_SKULL_SHAPE, StateTypes.PIGLIN_HEAD);
        fixed(CollisionShape.EMPTY, StateTypes.LIGHT);

        state(BlockShapes::snowLayers, StateTypes.SNOW);
        state(BlockShapes::cake, StateTypes.CAKE);
        state(BlockShapes::ladder, StateTypes.LADDER);
        state(BlockShapes::chest, StateTypes.CHEST, StateTypes.TRAPPED_CHEST);
        tag(BlockTags.COPPER_CHESTS, state(BlockShapes::chest));
        fixed(CHEST_SINGLE, StateTypes.ENDER_CHEST);
        state(BlockShapes::endPortalFrame, StateTypes.END_PORTAL_FRAME);
        state(s -> s.getEggs() > 1 ? TURTLE_EGG_MANY : TURTLE_EGG_ONE, StateTypes.TURTLE_EGG);
        state(BlockShapes::seaPickle, StateTypes.SEA_PICKLE);
        state(BlockShapes::hopperByFacing, StateTypes.HOPPER);
        state(BlockShapes::wallSkull, WALL_SKULLS.toArray(new StateType[0]));
        state(BlockShapes::piglinWallSkull, StateTypes.PIGLIN_WALL_HEAD);
        state(BlockShapes::bell, StateTypes.BELL);
        state(BlockShapes::grindstone, StateTypes.GRINDSTONE);
        state(BlockShapes::pistonBase, StateTypes.PISTON, StateTypes.STICKY_PISTON);
        state(BlockShapes::pistonHead, StateTypes.PISTON_HEAD);
        state(BlockShapes::chorusPlant, StateTypes.CHORUS_PLANT);
        state(BlockShapes::bigDripleaf, StateTypes.BIG_DRIPLEAF);
        state(amethyst(7, 3), StateTypes.AMETHYST_CLUSTER);
        state(amethyst(5, 3), StateTypes.LARGE_AMETHYST_BUD);
        state(amethyst(4, 3), StateTypes.MEDIUM_AMETHYST_BUD);
        state(amethyst(3, 4), StateTypes.SMALL_AMETHYST_BUD);
        state(BlockShapes::cocoa, StateTypes.COCOA);
        state(BlockShapes::pitcherCrop, StateTypes.PITCHER_CROP);
        state(s -> s.isBottom() ? CARPET_SHAPE : CollisionShape.EMPTY, StateTypes.PALE_MOSS_CARPET);
        state(BlockShapes::rod, StateTypes.END_ROD);
        state(BlockShapes::chain, StateTypes.CHAIN);

        RESOLVERS.put(StateTypes.BAMBOO, (state, x, y, z, ctx) -> offsetShape(BAMBOO_BOX, x, z, BAMBOO_MAX_OFFSET));
        RESOLVERS.put(StateTypes.SCAFFOLDING, BlockShapes::scaffolding);
        APPROXIMATE.add(StateTypes.SCAFFOLDING);

        fixed(CollisionShape.FULL, StateTypes.POWDER_SNOW);
        APPROXIMATE.add(StateTypes.POWDER_SNOW);

        fixed(CollisionShape.FULL, StateTypes.MOVING_PISTON);
        APPROXIMATE.add(StateTypes.MOVING_PISTON);

        SUFFOCATING_ALWAYS.add(StateTypes.SOUL_SAND);
        SUFFOCATING_ALWAYS.add(StateTypes.MUD);
        SUFFOCATING_ALWAYS.add(StateTypes.FARMLAND);
        SUFFOCATING_ALWAYS.add(StateTypes.DIRT_PATH);
        SUFFOCATING_ALWAYS.add(StateTypes.GRASS_PATH);

        BlockTags.LEAVES.getStates().forEach(SUFFOCATING_NEVER::add);
        SUFFOCATING_NEVER.add(StateTypes.GLASS);
        SUFFOCATING_NEVER.add(StateTypes.TINTED_GLASS);
        BlockTags.GLASS_BLOCKS.getStates().forEach(SUFFOCATING_NEVER::add);
        SUFFOCATING_NEVER.add(StateTypes.MANGROVE_ROOTS);
        SUFFOCATING_NEVER.add(StateTypes.MOVING_PISTON);
        SUFFOCATING_NEVER.add(StateTypes.COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.EXPOSED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WEATHERED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.OXIDIZED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_EXPOSED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_WEATHERED_COPPER_GRATE);
        SUFFOCATING_NEVER.add(StateTypes.WAXED_OXIDIZED_COPPER_GRATE);
    }

    private static ShapeResolver fixed(CollisionShape shape) {
        return (state, x, y, z, ctx) -> shape;
    }

    private static ShapeResolver state(java.util.function.Function<WrappedBlockState, CollisionShape> fn) {
        return (state, x, y, z, ctx) -> fn.apply(state);
    }

    private static void fixed(CollisionShape shape, StateType... types) {
        ShapeResolver resolver = fixed(shape);
        for (StateType type : types) RESOLVERS.put(type, resolver);
    }

    private static void state(java.util.function.Function<WrappedBlockState, CollisionShape> fn, StateType... types) {
        ShapeResolver resolver = state(fn);
        for (StateType type : types) RESOLVERS.put(type, resolver);
    }

    private static void tag(BlockTags tag, ShapeResolver resolver) {
        for (StateType type : tag.getStates()) RESOLVERS.put(type, resolver);
    }

    private static CollisionShape snowLayers(WrappedBlockState state) {
        int layers = Math.max(1, state.getLayers());
        if (layers <= 1) return CollisionShape.EMPTY;
        return CollisionShape.top((layers - 1) * 0.125);
    }

    private static CollisionShape cake(WrappedBlockState state) {
        return CollisionShape.box((1 + state.getBites() * 2) * PX, 0.0, PX, 15 * PX, 0.5, 15 * PX);
    }

    private static CollisionShape seaPickle(WrappedBlockState state) {
        return switch (state.getPickles()) {
            case 2 -> SEA_PICKLE_TWO;
            case 3 -> SEA_PICKLE_THREE;
            case 4 -> SEA_PICKLE_FOUR;
            default -> SEA_PICKLE_ONE;
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

    private static CollisionShape endPortalFrame(WrappedBlockState state) {
        return state.isEye() ? END_PORTAL_FRAME_EYE : END_PORTAL_FRAME_SHAPE;
    }

    private static CollisionShape ladder(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case NORTH -> pxShape(0, 0, 13, 16, 16, 16);
            case SOUTH -> pxShape(0, 0, 0, 16, 16, 3);
            case WEST -> pxShape(13, 0, 0, 16, 16, 16);
            case EAST -> pxShape(0, 0, 0, 3, 16, 16);
            default -> CollisionShape.EMPTY;
        };
    }

    private static CollisionShape chest(WrappedBlockState state) {
        Type type = state.getTypeData();
        boolean left = type == Type.LEFT;
        if (!left && type != Type.RIGHT) return CHEST_SINGLE;
        return switch (state.getFacing()) {
            case NORTH -> left ? CHEST_EAST : CHEST_WEST;
            case SOUTH -> left ? CHEST_WEST : CHEST_EAST;
            case WEST -> left ? CHEST_NORTH : CHEST_SOUTH;
            case EAST -> left ? CHEST_SOUTH : CHEST_NORTH;
            default -> CHEST_SINGLE;
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

    private static CollisionShape trapdoor(WrappedBlockState state) {
        if (!state.isOpen()) {
            return state.getHalf() == Half.TOP ? TRAPDOOR_TOP : TRAPDOOR_BOTTOM;
        }
        return switch (state.getFacing()) {
            case NORTH -> DOOR_NORTH;
            case SOUTH -> DOOR_SOUTH;
            case WEST -> DOOR_WEST;
            case EAST -> DOOR_EAST;
            default -> CollisionShape.EMPTY;
        };
    }

    private static CollisionShape bed(WrappedBlockState state) {
        boolean head = state.getPart() == Part.HEAD;
        return switch (state.getFacing()) {
            case NORTH -> head ? BED_NORTH : BED_SOUTH;
            case SOUTH -> head ? BED_SOUTH : BED_NORTH;
            case WEST -> head ? BED_WEST : BED_EAST;
            case EAST -> head ? BED_EAST : BED_WEST;
            default -> BED_NORTH;
        };
    }

    private static CollisionShape anvil(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case WEST, EAST -> ANVIL_X;
            default -> ANVIL_Z;
        };
    }

    private static CollisionShape hopper(CollisionBox spout) {
        List<CollisionBox> boxes = new ArrayList<>(7);
        for (CollisionBox box : HOPPER_BODY.boxes()) boxes.add(box);
        boxes.add(spout);
        return CollisionShape.of(boxes.toArray(new CollisionBox[0]));
    }

    private static CollisionShape hopperByFacing(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case DOWN -> HOPPER_DOWN;
            case NORTH -> HOPPER_NORTH;
            case SOUTH -> HOPPER_SOUTH;
            case WEST -> HOPPER_WEST;
            case EAST -> HOPPER_EAST;
            default -> HOPPER_DOWN;
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

    private static CollisionShape piglinWallSkull(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case SOUTH -> PIGLIN_WALL_SKULL_SOUTH;
            case WEST -> PIGLIN_WALL_SKULL_WEST;
            case EAST -> PIGLIN_WALL_SKULL_EAST;
            default -> PIGLIN_WALL_SKULL_NORTH;
        };
    }

    private static CollisionShape wallHangingSign(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case WEST, EAST -> HANGING_SIGN_PLANK_X;
            default -> HANGING_SIGN_PLANK_Z;
        };
    }

    private static CollisionShape shelf(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case SOUTH -> SHELF_SOUTH;
            case WEST -> SHELF_WEST;
            case EAST -> SHELF_EAST;
            default -> SHELF_NORTH;
        };
    }

    private static CollisionShape bell(WrappedBlockState state) {
        Attachment attachment = state.getAttachment();
        if (attachment == Attachment.FLOOR) {
            return switch (state.getFacing()) {
                case WEST, EAST -> BELL_FLOOR_X;
                default -> BELL_FLOOR_Z;
            };
        }
        if (attachment == Attachment.CEILING) return BELL_CEILING;
        if (attachment == Attachment.DOUBLE_WALL) {
            return switch (state.getFacing()) {
                case WEST, EAST -> BELL_DOUBLE_X;
                default -> BELL_DOUBLE_Z;
            };
        }
        return switch (state.getFacing()) {
            case SOUTH -> BELL_WALL_SOUTH;
            case WEST -> BELL_WALL_WEST;
            case EAST -> BELL_WALL_EAST;
            default -> BELL_WALL_NORTH;
        };
    }

    private static CollisionShape grindstone(WrappedBlockState state) {
        Face face = state.getFace();
        boolean axisX = switch (state.getFacing()) {
            case WEST, EAST -> true;
            default -> false;
        };
        if (face == Face.FLOOR) return axisX ? GRIND_FLOOR_X : GRIND_FLOOR_Z;
        if (face == Face.CEILING) return axisX ? GRIND_CEILING_X : GRIND_CEILING_Z;
        return switch (state.getFacing()) {
            case SOUTH -> GRIND_WALL_SOUTH;
            case WEST -> GRIND_WALL_WEST;
            case EAST -> GRIND_WALL_EAST;
            default -> GRIND_WALL_NORTH;
        };
    }

    private static CollisionShape pistonBase(WrappedBlockState state) {
        if (!state.isExtended()) return CollisionShape.FULL;
        return switch (state.getFacing()) {
            case UP -> PISTON_UP;
            case DOWN -> PISTON_DOWN;
            case NORTH -> PISTON_NORTH;
            case SOUTH -> PISTON_SOUTH;
            case WEST -> PISTON_WEST;
            default -> PISTON_EAST;
        };
    }

    private static CollisionShape pistonHead(WrappedBlockState state) {
        boolean shortArm = state.isShort();
        return switch (state.getFacing()) {
            case UP -> shortArm ? HEAD_UP_SHORT : HEAD_UP;
            case DOWN -> shortArm ? HEAD_DOWN_SHORT : HEAD_DOWN;
            case NORTH -> shortArm ? HEAD_NORTH_SHORT : HEAD_NORTH;
            case SOUTH -> shortArm ? HEAD_SOUTH_SHORT : HEAD_SOUTH;
            case WEST -> shortArm ? HEAD_WEST_SHORT : HEAD_WEST;
            default -> shortArm ? HEAD_EAST_SHORT : HEAD_EAST;
        };
    }

    private static CollisionShape chorusPlant(WrappedBlockState state) {
        List<CollisionBox> boxes = new ArrayList<>(7);
        boxes.add(PIPE_CORE);
        if (state.getNorth() != North.FALSE) boxes.add(PIPE_NORTH);
        if (state.getSouth() != South.FALSE) boxes.add(PIPE_SOUTH);
        if (state.getWest() != West.FALSE) boxes.add(PIPE_WEST);
        if (state.getEast() != East.FALSE) boxes.add(PIPE_EAST);
        if (state.isUp()) boxes.add(PIPE_UP);
        if (state.isDown()) boxes.add(PIPE_DOWN);
        return CollisionShape.of(boxes.toArray(new CollisionBox[0]));
    }

    private static CollisionShape bigDripleaf(WrappedBlockState state) {
        Tilt tilt = state.getTilt();
        if (tilt == Tilt.FULL) return CollisionShape.EMPTY;
        if (tilt == Tilt.PARTIAL) return BIG_DRIPLEAF_TILTED;
        return BIG_DRIPLEAF_FLAT;
    }

    private static java.util.function.Function<WrappedBlockState, CollisionShape> amethyst(double height, double inset) {
        double o = inset, h = height;
        CollisionShape up = pxShape(o, 0, o, 16 - o, h, 16 - o);
        CollisionShape down = pxShape(o, 16 - h, o, 16 - o, 16, 16 - o);
        CollisionShape north = pxShape(o, o, 16 - h, 16 - o, 16 - o, 16);
        CollisionShape south = pxShape(o, o, 0, 16 - o, 16 - o, h);
        CollisionShape east = pxShape(0, o, o, h, 16 - o, 16 - o);
        CollisionShape west = pxShape(16 - h, o, o, 16, 16 - o, 16 - o);
        return state -> switch (state.getFacing()) {
            case DOWN -> down;
            case NORTH -> north;
            case SOUTH -> south;
            case EAST -> east;
            case WEST -> west;
            default -> up;
        };
    }

    private static CollisionShape cocoa(WrappedBlockState state) {
        int facing = switch (state.getFacing()) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        int age = Math.min(2, Math.max(0, state.getAge()));
        return COCOA[facing][age];
    }

    private static CollisionShape pitcherCrop(WrappedBlockState state) {
        if (state.getHalf() != Half.LOWER) return CollisionShape.EMPTY;
        return state.getAge() == 0 ? PITCHER_CROP_BULB : PITCHER_CROP_GROWN;
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

    private static CollisionShape scaffolding(WrappedBlockState state, int x, int y, int z, CollisionContext ctx) {
        boolean above = ctx.feetY() > y + 1.0 - ABOVE_EPS;
        if (above && !ctx.descending()) return SCAFFOLD_STABLE;
        if (state.isBottom() && state.getDistance() != 0 && ctx.feetY() > y - ABOVE_EPS) return SCAFFOLD_BOTTOM;
        return CollisionShape.EMPTY;
    }

    private static CollisionShape speleothem(WrappedBlockState state, int x, int y, int z, CollisionContext ctx) {
        Thickness thickness = state.getThickness();
        CollisionBox box = switch (thickness) {
            case TIP_MERGE -> SPELEO_TIP_MERGE;
            case TIP -> state.getVerticalDirection() == VerticalDirection.DOWN ? SPELEO_TIP_DOWN : SPELEO_TIP_UP;
            case FRUSTUM -> SPELEO_FRUSTUM;
            case MIDDLE -> SPELEO_MIDDLE;
            default -> SPELEO_BASE;
        };
        return offsetShape(box, x, z, SPELEO_MAX_OFFSET);
    }

    private static CollisionShape offsetShape(CollisionBox box, int x, int z, float maxOffset) {
        long seed = (long) (x * 3129871) ^ (long) z * 116129781L;
        seed = seed * seed * 42317861L + seed * 11L;
        seed = seed >> 16;
        double dx = clampOffset(((float) (seed & 15L) / 15.0F - 0.5) * 0.5, maxOffset);
        double dz = clampOffset(((float) (seed >> 8 & 15L) / 15.0F - 0.5) * 0.5, maxOffset);
        return CollisionShape.box(box.minX() + dx, box.minY(), box.minZ() + dz,
                box.maxX() + dx, box.maxY(), box.maxZ() + dz);
    }

    private static double clampOffset(double value, float max) {
        return Math.max(-max, Math.min(max, value));
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
}
