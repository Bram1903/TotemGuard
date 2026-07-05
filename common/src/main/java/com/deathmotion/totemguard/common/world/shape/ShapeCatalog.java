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

package com.deathmotion.totemguard.common.world.shape;

// Box data extracted from the 26.2 client sources, authored in 1/16 pixel units through px().
public final class ShapeCatalog {

    public static final double[] EMPTY = {};
    public static final double[] FULL = {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};

    public static final double[] SOUL_SAND = top(0.875);
    public static final double[] MUD = top(0.875);
    public static final double[] PATH = top(0.9375);
    public static final double[] ENCHANTING_TABLE = top(0.75);
    public static final double[] DAYLIGHT_DETECTOR = top(0.375);
    public static final double[] DIODE = top(0.125);
    public static final double[] CARPET = top(0.0625);
    public static final double[] STONECUTTER = top(0.5625);
    public static final double[] CAMPFIRE = top(0.4375);
    public static final double[] SCULK_SENSOR = top(0.5);
    public static final double[] SLAB_BOTTOM = top(0.5);
    public static final double[] SLAB_TOP = {0.0, 0.5, 0.0, 1.0, 1.0, 1.0};
    public static final double[] TRAPDOOR_BOTTOM = top(0.1875);
    public static final double[] TRAPDOOR_TOP = px(0, 13, 0, 16, 16, 16);
    public static final double[] END_PORTAL_FRAME = top(0.8125);
    public static final double[] END_PORTAL_FRAME_EYE = px(
            0, 0, 0, 16, 13, 16,
            4, 13, 4, 12, 16, 12);

    public static final double[] HONEY_CACTUS = px(1, 0, 1, 15, 15, 15);
    public static final double[] LILY_PAD = px(1, 0, 1, 15, 1.5, 15);
    public static final double[] CHEST_SINGLE = px(1, 0, 1, 15, 14, 15);
    public static final double[] CHEST_NORTH = px(1, 0, 0, 15, 14, 15);
    public static final double[] CHEST_SOUTH = px(1, 0, 1, 15, 14, 16);
    public static final double[] CHEST_WEST = px(0, 0, 1, 15, 14, 15);
    public static final double[] CHEST_EAST = px(1, 0, 1, 16, 14, 15);
    public static final double[] FULL_HEIGHT_POT = px(1, 0, 1, 15, 16, 15);
    public static final double[] FLOWER_POT = px(5, 0, 5, 11, 6, 11);
    public static final double[] CONDUIT = px(5, 5, 5, 11, 11, 11);
    public static final double[] HEAVY_CORE = px(4, 0, 4, 12, 8, 12);
    public static final double[] GOLEM_STATUE = px(3, 0, 3, 13, 14, 13);
    public static final double[] DRIED_GHAST = px(3, 0, 3, 13, 10, 13);
    public static final double[] SNIFFER_EGG = px(1, 0, 2, 15, 16, 14);
    public static final double[] TURTLE_EGG_ONE = px(3, 0, 3, 12, 7, 12);
    public static final double[] TURTLE_EGG_MANY = px(1, 0, 1, 15, 7, 15);
    public static final double[] SKULL = px(4, 0, 4, 12, 8, 12);
    public static final double[] PIGLIN_SKULL = px(3, 0, 3, 13, 8, 13);

    public static final double[] SEA_PICKLE_ONE = px(6, 0, 6, 10, 6, 10);
    public static final double[] SEA_PICKLE_TWO = px(3, 0, 3, 13, 6, 13);
    public static final double[] SEA_PICKLE_THREE = px(2, 0, 2, 14, 6, 14);
    public static final double[] SEA_PICKLE_FOUR = px(2, 0, 2, 14, 7, 14);

    public static final double[] CANDLE_ONE = px(7, 0, 7, 9, 6, 9);
    public static final double[] CANDLE_TWO = px(5, 0, 6, 11, 6, 9);
    public static final double[] CANDLE_THREE = px(5, 0, 6, 10, 6, 11);
    public static final double[] CANDLE_FOUR = px(5, 0, 5, 11, 6, 10);

    public static final double[] CANDLE_CAKE = px(
            1, 0, 1, 15, 8, 15,
            7, 8, 7, 9, 14, 9);
    public static final double[] BREWING_STAND = px(
            1, 0, 1, 15, 2, 15,
            7, 2, 7, 9, 14, 9);
    public static final double[] LECTERN = px(
            0, 0, 0, 16, 2, 16,
            4, 2, 4, 12, 14, 12);

    public static final double[] PITCHER_CROP_BULB = px(5, -1, 5, 11, 3, 11);
    public static final double[] PITCHER_CROP_GROWN = px(3, -1, 3, 13, 5, 13);
    public static final double[] BIG_DRIPLEAF_FLAT = px(0, 11, 0, 16, 15, 16);
    public static final double[] BIG_DRIPLEAF_TILTED = px(0, 11, 0, 16, 13, 16);
    public static final double[] AZALEA = px(
            0, 8, 0, 16, 16, 16,
            6, 0, 6, 10, 8, 10);

    public static final double[] LANTERN_STANDING = px(
            5, 0, 5, 11, 7, 11,
            6, 7, 6, 10, 9, 10);
    public static final double[] LANTERN_HANGING = px(
            5, 1, 5, 11, 8, 11,
            6, 8, 6, 10, 10, 10);

    public static final double[] CHAIN_Y = px(6.5, 0, 6.5, 9.5, 16, 9.5);
    public static final double[] CHAIN_X = px(0, 6.5, 6.5, 16, 9.5, 9.5);
    public static final double[] CHAIN_Z = px(6.5, 6.5, 0, 9.5, 9.5, 16);
    public static final double[] ROD_Y = px(6, 0, 6, 10, 16, 10);
    public static final double[] ROD_X = px(0, 6, 6, 16, 10, 10);
    public static final double[] ROD_Z = px(6, 6, 0, 10, 10, 16);

    public static final double[] WALL_SKULL_NORTH = px(4, 4, 8, 12, 12, 16);
    public static final double[] WALL_SKULL_SOUTH = px(4, 4, 0, 12, 12, 8);
    public static final double[] WALL_SKULL_WEST = px(8, 4, 4, 16, 12, 12);
    public static final double[] WALL_SKULL_EAST = px(0, 4, 4, 8, 12, 12);
    public static final double[] PIGLIN_WALL_SKULL_NORTH = px(3, 4, 8, 13, 12, 16);
    public static final double[] PIGLIN_WALL_SKULL_SOUTH = px(3, 4, 0, 13, 12, 8);
    public static final double[] PIGLIN_WALL_SKULL_WEST = px(8, 4, 3, 16, 12, 13);
    public static final double[] PIGLIN_WALL_SKULL_EAST = px(0, 4, 3, 8, 12, 13);

    public static final double[] HANGING_SIGN_PLANK_Z = px(0, 14, 6, 16, 16, 10);
    public static final double[] HANGING_SIGN_PLANK_X = px(6, 14, 0, 10, 16, 16);

    public static final double[] DOOR_NORTH = px(0, 0, 13, 16, 16, 16);
    public static final double[] DOOR_SOUTH = px(0, 0, 0, 16, 16, 3);
    public static final double[] DOOR_WEST = px(13, 0, 0, 16, 16, 16);
    public static final double[] DOOR_EAST = px(0, 0, 0, 3, 16, 16);

    public static final double[] BED_NORTH = px(
            0, 3, 0, 16, 9, 16,
            0, 0, 0, 3, 3, 3,
            13, 0, 0, 16, 3, 3);
    public static final double[] BED_SOUTH = px(
            0, 3, 0, 16, 9, 16,
            0, 0, 13, 3, 3, 16,
            13, 0, 13, 16, 3, 16);
    public static final double[] BED_WEST = px(
            0, 3, 0, 16, 9, 16,
            0, 0, 0, 3, 3, 3,
            0, 0, 13, 3, 3, 16);
    public static final double[] BED_EAST = px(
            0, 3, 0, 16, 9, 16,
            13, 0, 0, 16, 3, 3,
            13, 0, 13, 16, 3, 16);

    public static final double[] CAULDRON = px(
            0, 3, 0, 16, 4, 16,
            0, 4, 0, 2, 16, 16,
            14, 4, 0, 16, 16, 16,
            2, 4, 0, 14, 16, 2,
            2, 4, 14, 14, 16, 16,
            0, 0, 0, 2, 3, 4,
            2, 0, 0, 4, 3, 2,
            14, 0, 0, 16, 3, 4,
            12, 0, 0, 14, 3, 2,
            0, 0, 12, 2, 3, 16,
            2, 0, 14, 4, 3, 16,
            14, 0, 12, 16, 3, 16,
            12, 0, 14, 14, 3, 16);

    public static final double[] COMPOSTER = px(
            0, 0, 0, 16, 2, 16,
            0, 2, 0, 2, 16, 16,
            14, 2, 0, 16, 16, 16,
            2, 2, 0, 14, 16, 2,
            2, 2, 14, 14, 16, 16);

    private static final double[] HOPPER_BODY = px(
            0, 10, 0, 2, 16, 16,
            14, 10, 0, 16, 16, 16,
            2, 10, 0, 14, 16, 2,
            2, 10, 14, 14, 16, 16,
            2, 10, 2, 14, 11, 14,
            4, 4, 4, 12, 10, 12);
    public static final double[] HOPPER_DOWN = concat(HOPPER_BODY, px(6, 0, 6, 10, 4, 10));
    public static final double[] HOPPER_NORTH = concat(HOPPER_BODY, px(6, 4, 0, 10, 8, 4));
    public static final double[] HOPPER_SOUTH = concat(HOPPER_BODY, px(6, 4, 12, 10, 8, 16));
    public static final double[] HOPPER_WEST = concat(HOPPER_BODY, px(0, 4, 6, 4, 8, 10));
    public static final double[] HOPPER_EAST = concat(HOPPER_BODY, px(12, 4, 6, 16, 8, 10));

    private static final double[] ANVIL_BASE = px(2, 0, 2, 14, 4, 14);
    public static final double[] ANVIL_X = concat(ANVIL_BASE, px(
            3, 4, 4, 13, 5, 12,
            4, 5, 6, 12, 10, 10,
            0, 10, 3, 16, 16, 13));
    public static final double[] ANVIL_Z = concat(ANVIL_BASE, px(
            4, 4, 3, 12, 5, 13,
            6, 5, 4, 10, 10, 12,
            3, 10, 0, 13, 16, 16));

    private static final double[] BELL_BODY = px(
            5, 6, 5, 11, 13, 11,
            4, 4, 4, 12, 6, 12);
    public static final double[] BELL_FLOOR_Z = px(0, 0, 4, 16, 16, 12);
    public static final double[] BELL_FLOOR_X = px(4, 0, 0, 12, 16, 16);
    public static final double[] BELL_CEILING = concat(BELL_BODY, px(7, 13, 7, 9, 16, 9));
    public static final double[] BELL_DOUBLE_Z = concat(BELL_BODY, px(7, 13, 0, 9, 15, 16));
    public static final double[] BELL_DOUBLE_X = concat(BELL_BODY, px(0, 13, 7, 16, 15, 9));
    public static final double[] BELL_WALL_NORTH = concat(BELL_BODY, px(7, 13, 0, 9, 15, 13));
    public static final double[] BELL_WALL_SOUTH = concat(BELL_BODY, px(7, 13, 3, 9, 15, 16));
    public static final double[] BELL_WALL_WEST = concat(BELL_BODY, px(0, 13, 7, 13, 15, 9));
    public static final double[] BELL_WALL_EAST = concat(BELL_BODY, px(3, 13, 7, 16, 15, 9));

    public static final double[] GRIND_WALL_NORTH = px(
            4, 2, 0, 12, 14, 12,
            2, 6, 7, 4, 10, 16,
            12, 6, 7, 14, 10, 16,
            2, 5, 3, 4, 11, 9,
            12, 5, 3, 14, 11, 9);
    public static final double[] GRIND_WALL_SOUTH = px(
            4, 2, 4, 12, 14, 16,
            2, 6, 0, 4, 10, 9,
            12, 6, 0, 14, 10, 9,
            2, 5, 7, 4, 11, 13,
            12, 5, 7, 14, 11, 13);
    public static final double[] GRIND_WALL_EAST = px(
            4, 2, 4, 16, 14, 12,
            0, 6, 2, 9, 10, 4,
            0, 6, 12, 9, 10, 14,
            7, 5, 2, 13, 11, 4,
            7, 5, 12, 13, 11, 14);
    public static final double[] GRIND_WALL_WEST = px(
            0, 2, 4, 12, 14, 12,
            7, 6, 2, 16, 10, 4,
            7, 6, 12, 16, 10, 14,
            3, 5, 2, 9, 11, 4,
            3, 5, 12, 9, 11, 14);
    public static final double[] GRIND_FLOOR_Z = px(
            4, 4, 2, 12, 16, 14,
            2, 0, 6, 4, 9, 10,
            12, 0, 6, 14, 9, 10,
            2, 7, 5, 4, 13, 11,
            12, 7, 5, 14, 13, 11);
    public static final double[] GRIND_FLOOR_X = px(
            2, 4, 4, 14, 16, 12,
            6, 0, 2, 10, 9, 4,
            6, 0, 12, 10, 9, 14,
            5, 7, 2, 11, 13, 4,
            5, 7, 12, 11, 13, 14);
    public static final double[] GRIND_CEILING_Z = px(
            4, 0, 2, 12, 12, 14,
            2, 7, 6, 4, 16, 10,
            12, 7, 6, 14, 16, 10,
            2, 3, 5, 4, 9, 11,
            12, 3, 5, 14, 9, 11);
    public static final double[] GRIND_CEILING_X = px(
            2, 0, 4, 14, 12, 12,
            6, 7, 2, 10, 16, 4,
            6, 7, 12, 10, 16, 14,
            5, 3, 2, 11, 9, 4,
            5, 3, 12, 11, 9, 14);

    public static final double[] PISTON_UP = top(0.75);
    public static final double[] PISTON_DOWN = px(0, 4, 0, 16, 16, 16);
    public static final double[] PISTON_NORTH = px(0, 0, 4, 16, 16, 16);
    public static final double[] PISTON_SOUTH = px(0, 0, 0, 16, 16, 12);
    public static final double[] PISTON_WEST = px(4, 0, 0, 16, 16, 16);
    public static final double[] PISTON_EAST = px(0, 0, 0, 12, 16, 16);

    public static final double[] HEAD_UP = px(0, 12, 0, 16, 16, 16, 6, -4, 6, 10, 12, 10);
    public static final double[] HEAD_UP_SHORT = px(0, 12, 0, 16, 16, 16, 6, 0, 6, 10, 12, 10);
    public static final double[] HEAD_DOWN = px(0, 0, 0, 16, 4, 16, 6, 4, 6, 10, 20, 10);
    public static final double[] HEAD_DOWN_SHORT = px(0, 0, 0, 16, 4, 16, 6, 4, 6, 10, 16, 10);
    public static final double[] HEAD_NORTH = px(0, 0, 0, 16, 16, 4, 6, 6, 4, 10, 10, 20);
    public static final double[] HEAD_NORTH_SHORT = px(0, 0, 0, 16, 16, 4, 6, 6, 4, 10, 10, 16);
    public static final double[] HEAD_SOUTH = px(0, 0, 12, 16, 16, 16, 6, 6, -4, 10, 10, 12);
    public static final double[] HEAD_SOUTH_SHORT = px(0, 0, 12, 16, 16, 16, 6, 6, 0, 10, 10, 12);
    public static final double[] HEAD_WEST = px(0, 0, 0, 4, 16, 16, 4, 6, 6, 20, 10, 10);
    public static final double[] HEAD_WEST_SHORT = px(0, 0, 0, 4, 16, 16, 4, 6, 6, 16, 10, 10);
    public static final double[] HEAD_EAST = px(12, 0, 0, 16, 16, 16, -4, 6, 6, 12, 10, 10);
    public static final double[] HEAD_EAST_SHORT = px(12, 0, 0, 16, 16, 16, 0, 6, 6, 12, 10, 10);

    public static final double[] PIPE_CORE = px(3, 3, 3, 13, 13, 13);
    public static final double[] PIPE_NORTH = px(3, 3, 0, 13, 13, 8);
    public static final double[] PIPE_SOUTH = px(3, 3, 8, 13, 13, 16);
    public static final double[] PIPE_WEST = px(0, 3, 3, 8, 13, 13);
    public static final double[] PIPE_EAST = px(8, 3, 3, 16, 13, 13);
    public static final double[] PIPE_DOWN = px(3, 0, 3, 13, 8, 13);
    public static final double[] PIPE_UP = px(3, 8, 3, 13, 16, 13);

    public static final double[] SCAFFOLD_STABLE = px(
            0, 14, 0, 16, 16, 16,
            0, 0, 0, 2, 16, 2,
            14, 0, 0, 16, 16, 2,
            0, 0, 14, 2, 16, 16,
            14, 0, 14, 16, 16, 16);
    public static final double[] SCAFFOLD_BOTTOM = top(0.125);

    public static final double[] POWDER_SNOW_FALLING = {0.0, 0.0, 0.0, 1.0, 0.9, 1.0};

    public static final double[] SPELEO_TIP_MERGE = px(5, 0, 5, 11, 16, 11);
    public static final double[] SPELEO_TIP_UP = px(5, 0, 5, 11, 11, 11);
    public static final double[] SPELEO_TIP_DOWN = px(5, 5, 5, 11, 16, 11);
    public static final double[] SPELEO_FRUSTUM = px(4, 0, 4, 12, 16, 12);
    public static final double[] SPELEO_MIDDLE = px(3, 0, 3, 13, 16, 13);
    public static final double[] SPELEO_BASE = px(2, 0, 2, 14, 16, 14);
    public static final float SPELEO_MAX_OFFSET = 0.125f;

    public static final double[] BAMBOO_BOX = px(6.5, 0, 6.5, 9.5, 16, 9.5);
    public static final float BAMBOO_MAX_OFFSET = 0.25f;

    public static final double[] SHELF_NORTH = px(
            0, 12, 11, 16, 16, 13,
            0, 0, 13, 16, 16, 16,
            0, 0, 11, 16, 4, 13);
    public static final double[] SHELF_EAST = px(
            3, 12, 0, 5, 16, 16,
            0, 0, 0, 3, 16, 16,
            3, 0, 0, 5, 4, 16);
    public static final double[] SHELF_SOUTH = px(
            0, 12, 3, 16, 16, 5,
            0, 0, 0, 16, 16, 3,
            0, 0, 3, 16, 4, 5);
    public static final double[] SHELF_WEST = px(
            11, 12, 0, 13, 16, 16,
            13, 0, 0, 16, 16, 16,
            11, 0, 0, 13, 4, 16);

    public static final double[][][] COCOA = {
            {px(6, 7, 1, 10, 12, 5), px(5, 5, 1, 11, 12, 7), px(4, 3, 1, 12, 12, 9)},
            {px(11, 7, 6, 15, 12, 10), px(9, 5, 5, 15, 12, 11), px(7, 3, 4, 15, 12, 12)},
            {px(6, 7, 11, 10, 12, 15), px(5, 5, 9, 11, 12, 15), px(4, 3, 7, 12, 12, 15)},
            {px(1, 7, 6, 5, 12, 10), px(1, 5, 5, 7, 12, 11), px(1, 3, 4, 9, 12, 12)}};

    private ShapeCatalog() {
    }

    public static double[] px(double... coords) {
        double[] out = new double[coords.length];
        for (int i = 0; i < coords.length; i++) out[i] = coords[i] / 16.0;
        return out;
    }

    public static double[] top(double maxY) {
        return new double[]{0.0, 0.0, 0.0, 1.0, maxY, 1.0};
    }

    public static double[] concat(double[] a, double[] b) {
        double[] out = new double[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static void emit(double[] boxes, int x, int y, int z, ShapeSink sink) {
        for (int i = 0; i < boxes.length; i += 6) {
            sink.accept(boxes[i] + x, boxes[i + 1] + y, boxes[i + 2] + z,
                    boxes[i + 3] + x, boxes[i + 4] + y, boxes[i + 5] + z);
        }
    }
}
