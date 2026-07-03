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

public final class MovementConstants {

    public static final double MAX_HORIZONTAL_FRICTION = 0.91;
    public static final double DEFAULT_SLIPPERINESS = 0.6;
    public static final double BASE_WIDTH = 0.6;
    public static final double STANDING_HEIGHT = 1.8;
    public static final double SNEAKING_HEIGHT = 1.5;
    public static final double SNEAKING_SPEED = 0.3;
    public static final double MOVEMENT_EFFICIENCY = 0.0;
    public static final double WATER_MOVEMENT_EFFICIENCY = 0.0;
    public static final double FLYING_SPEED = 0.4;
    public static final double SAFE_FALL_DISTANCE = 3.0;

    public static final double LEVITATION_PER_LEVEL = 0.05;
    public static final double LEVITATION_RATE = 0.2;

    public static final double GRAVITY = 0.08;
    public static final double SLOW_FALLING_GRAVITY = 0.01;
    public static final double VERTICAL_DRAG = 0.98;
    public static final double STEP_HEIGHT = 0.6;

    public static final double BASE_MOVEMENT_SPEED = 0.1;
    public static final double SPRINT_SPEED_MULTIPLIER = 1.3;
    public static final double GROUND_ACCEL_NUMERATOR = 0.21600002;
    public static final double AIR_ACCEL = 0.02;
    public static final double AIR_ACCEL_SPRINTING = 0.026;

    public static final double JUMP_POWER = 0.42;
    public static final double SPRINT_JUMP_BOOST = 0.2;
    public static final double WATER_EXIT_HOP = 0.3;

    public static final double WATER_FRICTION = 0.8;
    public static final double WATER_SPRINT_FRICTION = 0.9;
    public static final double WATER_DOLPHIN_FRICTION = 0.96;
    public static final double WATER_ACCEL = 0.02;
    public static final double WATER_EFFICIENCY_FRICTION_TARGET = 0.54600006;
    public static final double WATER_CURRENT_PUSH = 0.014;

    private MovementConstants() {
    }
}
