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

package com.deathmotion.totemguard.common.physics;

public final class MotionDefaults {

    public static final double GRAVITY = 0.08;
    public static final double VERTICAL_DRAG = 0.98f;
    public static final double JUMP_POWER = 0.42;
    public static final double STEP_HEIGHT = 0.6;
    public static final double FLUID_EXIT_HOP = 0.3f;

    public static final double BASE_WIDTH = 0.6;
    public static final double STANDING_HEIGHT = 1.8;
    public static final double SNEAKING_HEIGHT = 1.5;
    public static final double COMPACT_HEIGHT = 0.6;
    public static final double SLEEPING_SIZE = 0.2;

    public static final double STANDING_EYE_HEIGHT = 1.62;
    public static final double SNEAKING_EYE_HEIGHT = 1.27;
    public static final double COMPACT_EYE_HEIGHT = 0.4;
    public static final double SLEEPING_EYE_HEIGHT = 0.2;

    public static final double BASE_MOVEMENT_SPEED = 0.1;
    public static final double SNEAKING_SPEED = 0.3;
    public static final double INPUT_SCALE = 0.98f;
    public static final double MOVEMENT_EFFICIENCY = 0.0;
    public static final double WATER_MOVEMENT_EFFICIENCY = 0.0;
    public static final double FLYING_SPEED = 0.4;
    public static final double SAFE_FALL_DISTANCE = 3.0;

    private MotionDefaults() {
    }
}
