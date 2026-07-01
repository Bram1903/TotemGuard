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

public final class CollisionShape {

    public static final CollisionShape EMPTY = new CollisionShape(new CollisionBox[0]);
    public static final CollisionShape FULL = new CollisionShape(new CollisionBox[]{new CollisionBox(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)});

    private final CollisionBox[] boxes;

    private CollisionShape(CollisionBox[] boxes) {
        this.boxes = boxes;
    }

    public static CollisionShape of(CollisionBox... boxes) {
        return new CollisionShape(boxes);
    }

    public static CollisionShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new CollisionShape(new CollisionBox[]{new CollisionBox(minX, minY, minZ, maxX, maxY, maxZ)});
    }

    public static CollisionShape top(double maxY) {
        return box(0.0, 0.0, 0.0, 1.0, maxY, 1.0);
    }

    public boolean isEmpty() {
        return boxes.length == 0;
    }

    public double maxY() {
        double max = 0.0;
        for (CollisionBox box : boxes) {
            if (box.maxY() > max) max = box.maxY();
        }
        return max;
    }

    public double supportTop(double feetLimit, double minX, double maxX, double minZ, double maxZ) {
        double best = Double.NEGATIVE_INFINITY;
        for (CollisionBox box : boxes) {
            if (maxX <= box.minX() || minX >= box.maxX() || maxZ <= box.minZ() || minZ >= box.maxZ()) continue;
            double top = box.maxY();
            if (top <= feetLimit && top > best) best = top;
        }
        return best;
    }
}
