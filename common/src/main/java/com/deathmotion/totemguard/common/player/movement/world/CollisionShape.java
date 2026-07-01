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

package com.deathmotion.totemguard.common.player.movement.world;

public record CollisionShape(boolean present, double maxY) {

    public static final CollisionShape EMPTY = new CollisionShape(false, 0.0);
    public static final CollisionShape FULL = new CollisionShape(true, 1.0);

    public static CollisionShape top(double maxY) {
        return new CollisionShape(true, maxY);
    }

    public boolean isEmpty() {
        return !present;
    }
}
