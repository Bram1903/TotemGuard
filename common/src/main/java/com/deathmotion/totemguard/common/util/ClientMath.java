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

package com.deathmotion.totemguard.common.util;

public final class ClientMath {

    private static final double MODERN_SCALE = 10430.378350470453;
    private static final float LEGACY_SCALE = 10430.378f;
    private static final float FASTMATH_SCALE = 651.8986f;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    private static final float[] MODERN = new float[65536];
    private static final float[] LEGACY = new float[65536];
    private static final float[] FASTMATH = new float[4096];

    static {
        for (int i = 0; i < MODERN.length; i++) {
            MODERN[i] = (float) StrictMath.sin(i / MODERN_SCALE);
            LEGACY[i] = (float) StrictMath.sin(i * Math.PI * 2.0 / 65536.0);
        }
        for (int i = 0; i < FASTMATH.length; i++) {
            FASTMATH[i] = (float) StrictMath.sin(i * Math.PI * 2.0 / 4096.0);
        }
    }

    private ClientMath() {
    }

    public static float sin(float radians, boolean modern) {
        return modern
                ? MODERN[(int) ((long) (radians * MODERN_SCALE) & 65535L)]
                : LEGACY[(int) (radians * LEGACY_SCALE) & 0xFFFF];
    }

    public static float cos(float radians, boolean modern) {
        return modern
                ? MODERN[(int) ((long) (radians * MODERN_SCALE + 16384.0) & 65535L)]
                : LEGACY[(int) (radians * LEGACY_SCALE + 16384.0f) & 0xFFFF];
    }

    public static double lookX(float yaw, float pitch, boolean modern) {
        return sin(-yaw * DEG_TO_RAD, modern) * cos(pitch * DEG_TO_RAD, modern);
    }

    public static double lookY(float pitch, boolean modern) {
        return -sin(pitch * DEG_TO_RAD, modern);
    }

    public static double lookZ(float yaw, float pitch, boolean modern) {
        return cos(-yaw * DEG_TO_RAD, modern) * cos(pitch * DEG_TO_RAD, modern);
    }

    public static double horizontalDistance(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    public static float sinFast(float radians) {
        return FASTMATH[(int) (radians * FASTMATH_SCALE) & 4095];
    }

    public static float cosFast(float radians) {
        return FASTMATH[(int) (radians * FASTMATH_SCALE + 1024.0f) & 4095];
    }

    public static double lookXFast(float yaw, float pitch) {
        return sinFast(-yaw * DEG_TO_RAD) * cosFast(pitch * DEG_TO_RAD);
    }

    public static double lookYFast(float pitch) {
        return -sinFast(pitch * DEG_TO_RAD);
    }

    public static double lookZFast(float yaw, float pitch) {
        return cosFast(-yaw * DEG_TO_RAD) * cosFast(pitch * DEG_TO_RAD);
    }
}
