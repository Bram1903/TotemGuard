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

package com.deathmotion.totemguard.common.player.movement;

public final class MovementConstants {

    public static final double MAX_HORIZONTAL_FRICTION = 0.91;
    public static final double BASE_WIDTH = 0.6;
    public static final double STANDING_HEIGHT = 1.8;
    public static final double SNEAKING_HEIGHT = 1.5;

    public static final double LEVITATION_PER_LEVEL = 0.05;
    public static final double LEVITATION_RATE = 0.2;

    public static final double GRAVITY = 0.08;
    public static final double SLOW_FALLING_GRAVITY = 0.01;
    public static final double VERTICAL_DRAG = 0.98;
    public static final double STEP_HEIGHT = 0.6;

    private MovementConstants() {
    }
}
