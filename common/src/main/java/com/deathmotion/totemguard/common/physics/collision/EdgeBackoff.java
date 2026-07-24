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

package com.deathmotion.totemguard.common.physics.collision;

import com.deathmotion.totemguard.common.physics.VersionGates;

public final class EdgeBackoff {

    private static final double LEGACY_FALL_EPSILON = 1.0e-5;
    private static final double TIGHT_FALL_EPSILON = 1.0e-7;

    private final VersionGates gates;

    private ColliderBuffer buffer;
    private double minX;
    private double minZ;
    private double maxX;
    private double maxZ;
    private double fallMinY;
    private double fallMaxY;
    private double reachCap;
    private boolean usable;

    public EdgeBackoff(VersionGates gates) {
        this.gates = gates;
    }

    public void prepare(ColliderBuffer buffer, double startX, double startY, double startZ,
                        double halfWidth, double height, double stepHeight, double reachCap) {
        this.buffer = buffer;
        this.minX = startX - halfWidth;
        this.maxX = startX + halfWidth;
        this.minZ = startZ - halfWidth;
        this.maxZ = startZ + halfWidth;
        this.reachCap = reachCap;
        this.usable = stepHeight > 0.0;
        if (gates.edgeBackoffFeetOnly()) {
            double epsilon = gates.edgeBackoffTightEpsilon() ? TIGHT_FALL_EPSILON : LEGACY_FALL_EPSILON;
            this.fallMinY = startY - stepHeight - epsilon;
            this.fallMaxY = startY;
        } else {
            this.fallMinY = startY - stepHeight;
            this.fallMaxY = startY + height - stepHeight;
        }
    }

    public boolean clampsDisplacement(double centerX, double centerZ, double radius) {
        if (!usable || buffer == null) return false;
        double loX = capped(centerX - radius);
        double hiX = capped(centerX + radius);
        double loZ = capped(centerZ - radius);
        double hiZ = capped(centerZ + radius);
        return canFall(loX, 0.0) || canFall(hiX, 0.0)
                || canFall(0.0, loZ) || canFall(0.0, hiZ)
                || canFall(loX, loZ) || canFall(loX, hiZ)
                || canFall(hiX, loZ) || canFall(hiX, hiZ);
    }

    private double capped(double reach) {
        return Math.max(-reachCap, Math.min(reachCap, reach));
    }

    private boolean canFall(double dx, double dz) {
        if (dx == 0.0 && dz == 0.0) return false;
        double x0 = minX + dx;
        double x1 = maxX + dx;
        double z0 = minZ + dz;
        double z1 = maxZ + dz;
        int count = buffer.count();
        for (int i = 0; i < count; i++) {
            if (!ColliderBuffer.clipEligible(buffer.tagOf(i))) continue;
            if (!AxisClip.overlaps(x0, x1, buffer.minX(i), buffer.maxX(i))) continue;
            if (!AxisClip.overlaps(z0, z1, buffer.minZ(i), buffer.maxZ(i))) continue;
            if (!AxisClip.overlaps(fallMinY, fallMaxY, buffer.minY(i), buffer.maxY(i))) continue;
            return false;
        }
        return true;
    }
}
