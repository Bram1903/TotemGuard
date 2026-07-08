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

    private static final int MAX_SCENES = 4;
    private static final int SCENE_TICKS = 4;

    private final Scene[] scenes = new Scene[MAX_SCENES];
    private int count;

    public PistonData() {
        for (int i = 0; i < MAX_SCENES; i++) scenes[i] = new Scene();
    }

    public void arm(int dirX, int dirY, int dirZ, boolean slimeFront, boolean honeyFront,
                    int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (count >= MAX_SCENES) return;
        Scene scene = scenes[count++];
        scene.set(dirX, dirY, dirZ, slimeFront, honeyFront, minX, minY, minZ, maxX, maxY, maxZ);
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

    @Getter
    @Accessors(fluent = true)
    public static final class Scene {
        private int dirX, dirY, dirZ;
        private boolean slimeFront;
        private boolean honeyFront;
        private int minX, minY, minZ, maxX, maxY, maxZ;
        @Getter(AccessLevel.NONE)
        private int ticks;

        private void set(int dirX, int dirY, int dirZ, boolean slimeFront, boolean honeyFront,
                         int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.dirX = dirX;
            this.dirY = dirY;
            this.dirZ = dirZ;
            this.slimeFront = slimeFront;
            this.honeyFront = honeyFront;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.ticks = SCENE_TICKS;
        }
    }
}
