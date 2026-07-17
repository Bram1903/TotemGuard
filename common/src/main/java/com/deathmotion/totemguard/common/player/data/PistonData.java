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

package com.deathmotion.totemguard.common.player.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

public final class PistonData {

    public static final double PUSH_STEP = 0.51;
    public static final double SLIME_LAUNCH = 1.0;

    public static final int LAUNCH_NEG_X = 1;
    public static final int LAUNCH_POS_X = 1 << 1;
    public static final int LAUNCH_NEG_Y = 1 << 2;
    public static final int LAUNCH_POS_Y = 1 << 3;
    public static final int LAUNCH_NEG_Z = 1 << 4;
    public static final int LAUNCH_POS_Z = 1 << 5;

    private static final int MAX_SCENES = 16;
    private static final int SCENE_TICKS = 5;

    private final Scene[] scenes = new Scene[MAX_SCENES];
    private int count;

    public PistonData() {
        for (int i = 0; i < MAX_SCENES; i++) scenes[i] = new Scene();
    }

    private static int launchBit(int dirX, int dirY, int dirZ) {
        if (dirX < 0) return LAUNCH_NEG_X;
        if (dirX > 0) return LAUNCH_POS_X;
        if (dirY < 0) return LAUNCH_NEG_Y;
        if (dirY > 0) return LAUNCH_POS_Y;
        if (dirZ < 0) return LAUNCH_NEG_Z;
        return LAUNCH_POS_Z;
    }

    public Scene armScene() {
        if (count >= MAX_SCENES) mergeOldest();
        Scene scene = scenes[count++];
        scene.clear();
        scene.ticks = SCENE_TICKS;
        return scene;
    }

    public boolean isActive() {
        return count > 0;
    }

    public int sceneCount() {
        return count;
    }

    public Scene scene(int index) {
        return scenes[index];
    }

    public void tick() {
        int write = 0;
        for (int i = 0; i < count; i++) {
            Scene scene = scenes[i];
            if (--scene.ticks > 0) {
                if (write != i) {
                    Scene tmp = scenes[write];
                    scenes[write] = scene;
                    scenes[i] = tmp;
                }
                write++;
            }
        }
        count = write;
    }

    public void reset() {
        count = 0;
    }

    private void mergeOldest() {
        Scene first = scenes[0];
        Scene second = scenes[1];
        first.pushLoX = Math.min(first.pushLoX, second.pushLoX);
        first.pushHiX = Math.max(first.pushHiX, second.pushHiX);
        first.pushLoY = Math.min(first.pushLoY, second.pushLoY);
        first.pushHiY = Math.max(first.pushHiY, second.pushHiY);
        first.pushLoZ = Math.min(first.pushLoZ, second.pushLoZ);
        first.pushHiZ = Math.max(first.pushHiZ, second.pushHiZ);
        first.launchMask |= second.launchMask;
        first.minX = Math.min(first.minX, second.minX);
        first.minY = Math.min(first.minY, second.minY);
        first.minZ = Math.min(first.minZ, second.minZ);
        first.maxX = Math.max(first.maxX, second.maxX);
        first.maxY = Math.max(first.maxY, second.maxY);
        first.maxZ = Math.max(first.maxZ, second.maxZ);
        first.cellCount = -1;
        first.ticks = Math.max(first.ticks, second.ticks);
        for (int i = 2; i < count; i++) {
            Scene tmp = scenes[i - 1];
            scenes[i - 1] = scenes[i];
            scenes[i] = tmp;
        }
        scenes[count - 1] = second;
        count--;
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Scene {

        private static final int MAX_CELLS = 32;

        @Getter(AccessLevel.NONE)
        private final long[] cells = new long[MAX_CELLS];
        private double pushLoX, pushHiX;
        private double pushLoY, pushHiY;
        private double pushLoZ, pushHiZ;
        private int launchMask;
        private int minX, minY, minZ, maxX, maxY, maxZ;
        @Getter(AccessLevel.NONE)
        private int cellCount;
        @Getter(AccessLevel.NONE)
        private int ticks;

        private void clear() {
            pushLoX = 0.0;
            pushHiX = 0.0;
            pushLoY = 0.0;
            pushHiY = 0.0;
            pushLoZ = 0.0;
            pushHiZ = 0.0;
            launchMask = 0;
            minX = Integer.MAX_VALUE;
            minY = Integer.MAX_VALUE;
            minZ = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE;
            maxY = Integer.MIN_VALUE;
            maxZ = Integer.MIN_VALUE;
            cellCount = 0;
        }

        public void push(int dirX, int dirY, int dirZ, boolean symmetric) {
            double stepX = PUSH_STEP * dirX;
            double stepY = PUSH_STEP * dirY;
            double stepZ = PUSH_STEP * dirZ;
            pushLoX = Math.min(pushLoX, symmetric ? -Math.abs(stepX) : Math.min(0.0, stepX));
            pushHiX = Math.max(pushHiX, symmetric ? Math.abs(stepX) : Math.max(0.0, stepX));
            pushLoY = Math.min(pushLoY, symmetric ? -Math.abs(stepY) : Math.min(0.0, stepY));
            pushHiY = Math.max(pushHiY, symmetric ? Math.abs(stepY) : Math.max(0.0, stepY));
            pushLoZ = Math.min(pushLoZ, symmetric ? -Math.abs(stepZ) : Math.min(0.0, stepZ));
            pushHiZ = Math.max(pushHiZ, symmetric ? Math.abs(stepZ) : Math.max(0.0, stepZ));
        }

        public void launch(int dirX, int dirY, int dirZ) {
            launchMask |= launchBit(dirX, dirY, dirZ);
        }

        public void addCell(int x, int y, int z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
            if (cellCount < 0) return;
            if (cellCount >= MAX_CELLS) {
                cellCount = -1;
                return;
            }
            cells[cellCount++] = packCell(x, y, z);
        }

        public void saturate(int pistonX, int pistonY, int pistonZ, int radius) {
            minX = Math.min(minX, pistonX - radius);
            minY = Math.min(minY, pistonY - radius);
            minZ = Math.min(minZ, pistonZ - radius);
            maxX = Math.max(maxX, pistonX + radius);
            maxY = Math.max(maxY, pistonY + radius);
            maxZ = Math.max(maxZ, pistonZ + radius);
        }

        public boolean cellsExact() {
            return cellCount >= 0;
        }

        public int cellCount() {
            return Math.max(cellCount, 0);
        }

        public long cell(int index) {
            return cells[index];
        }

        public static int cellX(long cell) {
            return (int) (cell >> 38);
        }

        public static int cellY(long cell) {
            return (int) (cell << 52 >> 52);
        }

        public static int cellZ(long cell) {
            return (int) (cell << 26 >> 38);
        }

        public static long packCell(int x, int y, int z) {
            return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
        }
    }
}
