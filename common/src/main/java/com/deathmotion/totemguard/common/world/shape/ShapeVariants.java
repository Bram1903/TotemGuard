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

import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.enums.*;

import static com.deathmotion.totemguard.common.world.shape.ShapeCatalog.emit;

final class ShapeVariants {

    private static final double FENCE_TOP = 1.5;
    private static final double FENCE_ARM_HALF = 0.125;
    private static final double PANE_ARM_HALF = 0.0625;
    private static final double WALL_POST_HALF = 0.25;
    private static final double WALL_ARM_HALF = 0.1875;

    private static final int[] STAIR_QUADRANTS_BY_STATE = {12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    private static final double[][] AMETHYST_CLUSTER = amethyst(7, 3);
    private static final double[][] LARGE_AMETHYST_BUD = amethyst(5, 3);
    private static final double[][] MEDIUM_AMETHYST_BUD = amethyst(4, 3);
    private static final double[][] SMALL_AMETHYST_BUD = amethyst(3, 4);

    private ShapeVariants() {
    }

    static double[] slab(WrappedBlockState state) {
        return switch (state.getTypeData()) {
            case TOP -> ShapeCatalog.SLAB_TOP;
            case DOUBLE -> ShapeCatalog.FULL;
            default -> ShapeCatalog.SLAB_BOTTOM;
        };
    }

    static double[] snowLayers(WrappedBlockState state) {
        int layers = Math.max(1, state.getLayers());
        if (layers <= 1) return ShapeCatalog.EMPTY;
        return ShapeCatalog.top((layers - 1) * 0.125);
    }

    static void cake(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        double minX = (1 + state.getBites() * 2) / 16.0;
        sink.accept(x + minX, y, z + 1.0 / 16.0, x + 15.0 / 16.0, y + 0.5, z + 15.0 / 16.0);
    }

    static double[] seaPickle(WrappedBlockState state) {
        return switch (state.getPickles()) {
            case 2 -> ShapeCatalog.SEA_PICKLE_TWO;
            case 3 -> ShapeCatalog.SEA_PICKLE_THREE;
            case 4 -> ShapeCatalog.SEA_PICKLE_FOUR;
            default -> ShapeCatalog.SEA_PICKLE_ONE;
        };
    }

    static double[] candles(WrappedBlockState state) {
        return switch (state.getCandles()) {
            case 2 -> ShapeCatalog.CANDLE_TWO;
            case 3 -> ShapeCatalog.CANDLE_THREE;
            case 4 -> ShapeCatalog.CANDLE_FOUR;
            default -> ShapeCatalog.CANDLE_ONE;
        };
    }

    static double[] turtleEgg(WrappedBlockState state) {
        return state.getEggs() > 1 ? ShapeCatalog.TURTLE_EGG_MANY : ShapeCatalog.TURTLE_EGG_ONE;
    }

    static double[] endPortalFrame(WrappedBlockState state) {
        return state.isEye() ? ShapeCatalog.END_PORTAL_FRAME_EYE : ShapeCatalog.END_PORTAL_FRAME;
    }

    static double[] ladder(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case NORTH -> ShapeCatalog.DOOR_NORTH;
            case SOUTH -> ShapeCatalog.DOOR_SOUTH;
            case WEST -> ShapeCatalog.DOOR_WEST;
            case EAST -> ShapeCatalog.DOOR_EAST;
            default -> ShapeCatalog.EMPTY;
        };
    }

    static double[] chest(WrappedBlockState state) {
        Type type = state.getTypeData();
        boolean left = type == Type.LEFT;
        if (!left && type != Type.RIGHT) return ShapeCatalog.CHEST_SINGLE;
        return switch (state.getFacing()) {
            case NORTH -> left ? ShapeCatalog.CHEST_EAST : ShapeCatalog.CHEST_WEST;
            case SOUTH -> left ? ShapeCatalog.CHEST_WEST : ShapeCatalog.CHEST_EAST;
            case WEST -> left ? ShapeCatalog.CHEST_NORTH : ShapeCatalog.CHEST_SOUTH;
            case EAST -> left ? ShapeCatalog.CHEST_SOUTH : ShapeCatalog.CHEST_NORTH;
            default -> ShapeCatalog.CHEST_SINGLE;
        };
    }

    static double[] door(WrappedBlockState state) {
        boolean closed = !state.isOpen();
        boolean rightHinge = state.getHinge() == Hinge.RIGHT;
        return switch (state.getFacing()) {
            case SOUTH ->
                    closed ? ShapeCatalog.DOOR_SOUTH : (rightHinge ? ShapeCatalog.DOOR_EAST : ShapeCatalog.DOOR_WEST);
            case WEST ->
                    closed ? ShapeCatalog.DOOR_WEST : (rightHinge ? ShapeCatalog.DOOR_SOUTH : ShapeCatalog.DOOR_NORTH);
            case NORTH ->
                    closed ? ShapeCatalog.DOOR_NORTH : (rightHinge ? ShapeCatalog.DOOR_WEST : ShapeCatalog.DOOR_EAST);
            default ->
                    closed ? ShapeCatalog.DOOR_EAST : (rightHinge ? ShapeCatalog.DOOR_NORTH : ShapeCatalog.DOOR_SOUTH);
        };
    }

    static double[] trapdoor(WrappedBlockState state) {
        if (!state.isOpen()) {
            return state.getHalf() == Half.TOP ? ShapeCatalog.TRAPDOOR_TOP : ShapeCatalog.TRAPDOOR_BOTTOM;
        }
        return switch (state.getFacing()) {
            case NORTH -> ShapeCatalog.DOOR_NORTH;
            case SOUTH -> ShapeCatalog.DOOR_SOUTH;
            case WEST -> ShapeCatalog.DOOR_WEST;
            case EAST -> ShapeCatalog.DOOR_EAST;
            default -> ShapeCatalog.EMPTY;
        };
    }

    static double[] bed(WrappedBlockState state) {
        boolean head = state.getPart() == Part.HEAD;
        return switch (state.getFacing()) {
            case NORTH -> head ? ShapeCatalog.BED_NORTH : ShapeCatalog.BED_SOUTH;
            case SOUTH -> head ? ShapeCatalog.BED_SOUTH : ShapeCatalog.BED_NORTH;
            case WEST -> head ? ShapeCatalog.BED_WEST : ShapeCatalog.BED_EAST;
            case EAST -> head ? ShapeCatalog.BED_EAST : ShapeCatalog.BED_WEST;
            default -> ShapeCatalog.BED_NORTH;
        };
    }

    static double[] anvil(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case WEST, EAST -> ShapeCatalog.ANVIL_X;
            default -> ShapeCatalog.ANVIL_Z;
        };
    }

    static double[] lantern(WrappedBlockState state) {
        return state.isHanging() ? ShapeCatalog.LANTERN_HANGING : ShapeCatalog.LANTERN_STANDING;
    }

    static double[] hopper(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case NORTH -> ShapeCatalog.HOPPER_NORTH;
            case SOUTH -> ShapeCatalog.HOPPER_SOUTH;
            case WEST -> ShapeCatalog.HOPPER_WEST;
            case EAST -> ShapeCatalog.HOPPER_EAST;
            default -> ShapeCatalog.HOPPER_DOWN;
        };
    }

    static double[] wallSkull(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case SOUTH -> ShapeCatalog.WALL_SKULL_SOUTH;
            case WEST -> ShapeCatalog.WALL_SKULL_WEST;
            case EAST -> ShapeCatalog.WALL_SKULL_EAST;
            default -> ShapeCatalog.WALL_SKULL_NORTH;
        };
    }

    static double[] piglinWallSkull(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case SOUTH -> ShapeCatalog.PIGLIN_WALL_SKULL_SOUTH;
            case WEST -> ShapeCatalog.PIGLIN_WALL_SKULL_WEST;
            case EAST -> ShapeCatalog.PIGLIN_WALL_SKULL_EAST;
            default -> ShapeCatalog.PIGLIN_WALL_SKULL_NORTH;
        };
    }

    static double[] wallHangingSign(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case WEST, EAST -> ShapeCatalog.HANGING_SIGN_PLANK_X;
            default -> ShapeCatalog.HANGING_SIGN_PLANK_Z;
        };
    }

    static double[] shelf(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case SOUTH -> ShapeCatalog.SHELF_SOUTH;
            case WEST -> ShapeCatalog.SHELF_WEST;
            case EAST -> ShapeCatalog.SHELF_EAST;
            default -> ShapeCatalog.SHELF_NORTH;
        };
    }

    static double[] bell(WrappedBlockState state) {
        Attachment attachment = state.getAttachment();
        if (attachment == Attachment.FLOOR) {
            return switch (state.getFacing()) {
                case WEST, EAST -> ShapeCatalog.BELL_FLOOR_X;
                default -> ShapeCatalog.BELL_FLOOR_Z;
            };
        }
        if (attachment == Attachment.CEILING) return ShapeCatalog.BELL_CEILING;
        if (attachment == Attachment.DOUBLE_WALL) {
            return switch (state.getFacing()) {
                case WEST, EAST -> ShapeCatalog.BELL_DOUBLE_X;
                default -> ShapeCatalog.BELL_DOUBLE_Z;
            };
        }
        return switch (state.getFacing()) {
            case SOUTH -> ShapeCatalog.BELL_WALL_SOUTH;
            case WEST -> ShapeCatalog.BELL_WALL_WEST;
            case EAST -> ShapeCatalog.BELL_WALL_EAST;
            default -> ShapeCatalog.BELL_WALL_NORTH;
        };
    }

    static double[] grindstone(WrappedBlockState state) {
        Face face = state.getFace();
        boolean axisX = switch (state.getFacing()) {
            case WEST, EAST -> true;
            default -> false;
        };
        if (face == Face.FLOOR) return axisX ? ShapeCatalog.GRIND_FLOOR_X : ShapeCatalog.GRIND_FLOOR_Z;
        if (face == Face.CEILING) return axisX ? ShapeCatalog.GRIND_CEILING_X : ShapeCatalog.GRIND_CEILING_Z;
        return switch (state.getFacing()) {
            case SOUTH -> ShapeCatalog.GRIND_WALL_SOUTH;
            case WEST -> ShapeCatalog.GRIND_WALL_WEST;
            case EAST -> ShapeCatalog.GRIND_WALL_EAST;
            default -> ShapeCatalog.GRIND_WALL_NORTH;
        };
    }

    static double[] pistonBase(WrappedBlockState state) {
        if (!state.isExtended()) return ShapeCatalog.FULL;
        return switch (state.getFacing()) {
            case UP -> ShapeCatalog.PISTON_UP;
            case DOWN -> ShapeCatalog.PISTON_DOWN;
            case NORTH -> ShapeCatalog.PISTON_NORTH;
            case SOUTH -> ShapeCatalog.PISTON_SOUTH;
            case WEST -> ShapeCatalog.PISTON_WEST;
            default -> ShapeCatalog.PISTON_EAST;
        };
    }

    static double[] pistonHead(WrappedBlockState state) {
        boolean shortArm = state.isShort();
        return switch (state.getFacing()) {
            case UP -> shortArm ? ShapeCatalog.HEAD_UP_SHORT : ShapeCatalog.HEAD_UP;
            case DOWN -> shortArm ? ShapeCatalog.HEAD_DOWN_SHORT : ShapeCatalog.HEAD_DOWN;
            case NORTH -> shortArm ? ShapeCatalog.HEAD_NORTH_SHORT : ShapeCatalog.HEAD_NORTH;
            case SOUTH -> shortArm ? ShapeCatalog.HEAD_SOUTH_SHORT : ShapeCatalog.HEAD_SOUTH;
            case WEST -> shortArm ? ShapeCatalog.HEAD_WEST_SHORT : ShapeCatalog.HEAD_WEST;
            default -> shortArm ? ShapeCatalog.HEAD_EAST_SHORT : ShapeCatalog.HEAD_EAST;
        };
    }

    static double[] bigDripleaf(WrappedBlockState state) {
        Tilt tilt = state.getTilt();
        if (tilt == Tilt.FULL) return ShapeCatalog.EMPTY;
        if (tilt == Tilt.PARTIAL) return ShapeCatalog.BIG_DRIPLEAF_TILTED;
        return ShapeCatalog.BIG_DRIPLEAF_FLAT;
    }

    static double[] cocoa(WrappedBlockState state) {
        int facing = switch (state.getFacing()) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        int age = Math.min(2, Math.max(0, state.getAge()));
        return ShapeCatalog.COCOA[facing][age];
    }

    static double[] pitcherCrop(WrappedBlockState state) {
        if (state.getHalf() != Half.LOWER) return ShapeCatalog.EMPTY;
        return state.getAge() == 0 ? ShapeCatalog.PITCHER_CROP_BULB : ShapeCatalog.PITCHER_CROP_GROWN;
    }

    static double[] chain(WrappedBlockState state) {
        return switch (state.getAxis()) {
            case X -> ShapeCatalog.CHAIN_X;
            case Z -> ShapeCatalog.CHAIN_Z;
            default -> ShapeCatalog.CHAIN_Y;
        };
    }

    static double[] rod(WrappedBlockState state) {
        return switch (state.getFacing()) {
            case NORTH, SOUTH -> ShapeCatalog.ROD_Z;
            case WEST, EAST -> ShapeCatalog.ROD_X;
            default -> ShapeCatalog.ROD_Y;
        };
    }

    static double[] amethystShape(WrappedBlockState state, double[][] byDirection) {
        return switch (state.getFacing()) {
            case DOWN -> byDirection[1];
            case NORTH -> byDirection[2];
            case SOUTH -> byDirection[3];
            case EAST -> byDirection[4];
            case WEST -> byDirection[5];
            default -> byDirection[0];
        };
    }

    static double[][] amethystCluster() {
        return AMETHYST_CLUSTER;
    }

    static double[][] largeAmethystBud() {
        return LARGE_AMETHYST_BUD;
    }

    static double[][] mediumAmethystBud() {
        return MEDIUM_AMETHYST_BUD;
    }

    static double[][] smallAmethystBud() {
        return SMALL_AMETHYST_BUD;
    }

    static void stairs(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        int facing = switch (state.getFacing()) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> -1;
        };
        if (facing < 0) {
            emit(ShapeCatalog.FULL, x, y, z, sink);
            return;
        }
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

        if (top) {
            sink.accept(x, y + 0.5, z, x + 1.0, y + 1.0, z + 1.0);
        } else {
            sink.accept(x, y, z, x + 1.0, y + 0.5, z + 1.0);
        }
        if ((quadrants & 1) != 0) sink.accept(x, y + stepY0, z, x + 0.5, y + stepY1, z + 0.5);
        if ((quadrants & 2) != 0) sink.accept(x + 0.5, y + stepY0, z, x + 1.0, y + stepY1, z + 0.5);
        if ((quadrants & 4) != 0) sink.accept(x, y + stepY0, z + 0.5, x + 0.5, y + stepY1, z + 1.0);
        if ((quadrants & 8) != 0) sink.accept(x + 0.5, y + stepY0, z + 0.5, x + 1.0, y + stepY1, z + 1.0);
    }

    static void fence(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        cross(true, FENCE_ARM_HALF, FENCE_ARM_HALF, FENCE_TOP,
                state.getNorth() != North.FALSE, state.getSouth() != South.FALSE,
                state.getWest() != West.FALSE, state.getEast() != East.FALSE,
                x, y, z, sink);
    }

    static void pane(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        cross(true, PANE_ARM_HALF, PANE_ARM_HALF, 1.0,
                state.getNorth() != North.FALSE, state.getSouth() != South.FALSE,
                state.getWest() != West.FALSE, state.getEast() != East.FALSE,
                x, y, z, sink);
    }

    static void wall(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        cross(state.isUp(), WALL_POST_HALF, WALL_ARM_HALF, FENCE_TOP,
                state.getNorth() != North.NONE && state.getNorth() != North.FALSE,
                state.getSouth() != South.NONE && state.getSouth() != South.FALSE,
                state.getWest() != West.NONE && state.getWest() != West.FALSE,
                state.getEast() != East.NONE && state.getEast() != East.FALSE,
                x, y, z, sink);
    }

    static void fenceGate(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        if (state.isOpen()) return;
        switch (state.getFacing()) {
            case NORTH, SOUTH ->
                    sink.accept(x, y, z + 0.5 - FENCE_ARM_HALF, x + 1.0, y + FENCE_TOP, z + 0.5 + FENCE_ARM_HALF);
            default -> sink.accept(x + 0.5 - FENCE_ARM_HALF, y, z, x + 0.5 + FENCE_ARM_HALF, y + FENCE_TOP, z + 1.0);
        }
    }

    static void chorusPlant(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        emit(ShapeCatalog.PIPE_CORE, x, y, z, sink);
        if (state.getNorth() != North.FALSE) emit(ShapeCatalog.PIPE_NORTH, x, y, z, sink);
        if (state.getSouth() != South.FALSE) emit(ShapeCatalog.PIPE_SOUTH, x, y, z, sink);
        if (state.getWest() != West.FALSE) emit(ShapeCatalog.PIPE_WEST, x, y, z, sink);
        if (state.getEast() != East.FALSE) emit(ShapeCatalog.PIPE_EAST, x, y, z, sink);
        if (state.isUp()) emit(ShapeCatalog.PIPE_UP, x, y, z, sink);
        if (state.isDown()) emit(ShapeCatalog.PIPE_DOWN, x, y, z, sink);
    }

    static void scaffolding(WrappedBlockState state, int x, int y, int z, ShapeQuery query, ShapeSink sink) {
        if (query.above(y, 1.0) && !query.sneaking()) {
            emit(ShapeCatalog.SCAFFOLD_STABLE, x, y, z, sink);
            return;
        }
        if (state.isBottom() && state.getDistance() != 0 && query.above(y, 0.0)) {
            emit(ShapeCatalog.SCAFFOLD_BOTTOM, x, y, z, sink);
        }
    }

    static void powderSnow(int x, int y, int z, ShapeQuery query, ShapeSink sink) {
        if (query.deepFall()) {
            emit(ShapeCatalog.POWDER_SNOW_FALLING, x, y, z, sink);
            return;
        }
        if (query.standsOnPowderSnow() && query.above(y, 1.0) && !query.sneaking()) {
            emit(ShapeCatalog.FULL, x, y, z, sink);
        }
    }

    static void speleothem(WrappedBlockState state, int x, int y, int z, ShapeSink sink) {
        Thickness thickness = state.getThickness();
        double[] box = switch (thickness) {
            case TIP_MERGE -> ShapeCatalog.SPELEO_TIP_MERGE;
            case TIP -> state.getVerticalDirection() == VerticalDirection.DOWN
                    ? ShapeCatalog.SPELEO_TIP_DOWN : ShapeCatalog.SPELEO_TIP_UP;
            case FRUSTUM -> ShapeCatalog.SPELEO_FRUSTUM;
            case MIDDLE -> ShapeCatalog.SPELEO_MIDDLE;
            default -> ShapeCatalog.SPELEO_BASE;
        };
        offsetBox(box, x, y, z, ShapeCatalog.SPELEO_MAX_OFFSET, sink);
    }

    static void bamboo(int x, int y, int z, ShapeSink sink) {
        offsetBox(ShapeCatalog.BAMBOO_BOX, x, y, z, ShapeCatalog.BAMBOO_MAX_OFFSET, sink);
    }

    private static void offsetBox(double[] box, int x, int y, int z, float maxOffset, ShapeSink sink) {
        long seed = (long) (x * 3129871) ^ (long) z * 116129781L;
        seed = seed * seed * 42317861L + seed * 11L;
        seed = seed >> 16;
        double dx = clampOffset(((float) (seed & 15L) / 15.0F - 0.5) * 0.5, maxOffset);
        double dz = clampOffset(((float) (seed >> 8 & 15L) / 15.0F - 0.5) * 0.5, maxOffset);
        sink.accept(box[0] + x + dx, box[1] + y, box[2] + z + dz,
                box[3] + x + dx, box[4] + y, box[5] + z + dz);
    }

    private static double clampOffset(double value, float max) {
        return Math.max(-max, Math.min(max, value));
    }

    private static void cross(boolean post, double postHalf, double armHalf, double height,
                              boolean north, boolean south, boolean west, boolean east,
                              int x, int y, int z, ShapeSink sink) {
        double p0 = 0.5 - postHalf, p1 = 0.5 + postHalf;
        double a0 = 0.5 - armHalf, a1 = 0.5 + armHalf;
        if (post) sink.accept(x + p0, y, z + p0, x + p1, y + height, z + p1);
        if (north) sink.accept(x + a0, y, z, x + a1, y + height, z + a1);
        if (south) sink.accept(x + a0, y, z + a0, x + a1, y + height, z + 1.0);
        if (west) sink.accept(x, y, z + a0, x + a1, y + height, z + a1);
        if (east) sink.accept(x + a0, y, z + a0, x + 1.0, y + height, z + a1);
    }

    private static double[][] amethyst(double height, double inset) {
        double o = inset, h = height;
        double[] up = ShapeCatalog.px(o, 0, o, 16 - o, h, 16 - o);
        double[] down = ShapeCatalog.px(o, 16 - h, o, 16 - o, 16, 16 - o);
        double[] north = ShapeCatalog.px(o, o, 16 - h, 16 - o, 16 - o, 16);
        double[] south = ShapeCatalog.px(o, o, 0, 16 - o, 16 - o, h);
        double[] east = ShapeCatalog.px(0, o, o, h, 16 - o, 16 - o);
        double[] west = ShapeCatalog.px(16 - h, o, o, 16, 16 - o, 16 - o);
        return new double[][]{up, down, north, south, east, west};
    }
}
