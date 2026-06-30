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

import com.github.retrooper.packetevents.protocol.world.Location;

public record BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    public static BoundingBox player(Location feet, double width, double height) {
        double half = width / 2.0;
        return new BoundingBox(
                feet.getX() - half, feet.getY(), feet.getZ() - half,
                feet.getX() + half, feet.getY() + height, feet.getZ() + half);
    }

    public static BoundingBox sweptPlayer(Location current, Location previous, double width, double height) {
        return player(current, width, height).stretchTowards(
                previous.getX() - current.getX(),
                previous.getY() - current.getY(),
                previous.getZ() - current.getZ());
    }

    public BoundingBox stretchTowards(double dx, double dy, double dz) {
        double newMinX = dx < 0 ? minX + dx : minX;
        double newMaxX = dx > 0 ? maxX + dx : maxX;
        double newMinY = dy < 0 ? minY + dy : minY;
        double newMaxY = dy > 0 ? maxY + dy : maxY;
        double newMinZ = dz < 0 ? minZ + dz : minZ;
        double newMaxZ = dz > 0 ? maxZ + dz : maxZ;
        return new BoundingBox(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }
}
