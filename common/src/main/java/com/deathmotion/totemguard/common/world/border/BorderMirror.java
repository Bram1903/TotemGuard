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

package com.deathmotion.totemguard.common.world.border;

public final class BorderMirror {

    private static final double DEFAULT_HALF_EXTENT = 2.9999984E7;

    private double centerX;
    private double centerZ;
    private double halfExtent = DEFAULT_HALF_EXTENT;

    private double lerpFromHalfExtent;
    private double lerpToHalfExtent;
    private long lerpStartNanos;
    private long lerpDurationNanos;

    public void initialize(double centerX, double centerZ, double diameter) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        setDiameter(diameter);
    }

    public void setCenter(double centerX, double centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public void setDiameter(double diameter) {
        this.halfExtent = clampHalfExtent(diameter);
        this.lerpDurationNanos = 0;
    }

    public void lerpDiameter(double fromDiameter, double toDiameter, long durationMillis) {
        if (durationMillis <= 0) {
            setDiameter(toDiameter);
            return;
        }
        this.lerpFromHalfExtent = clampHalfExtent(fromDiameter);
        this.lerpToHalfExtent = clampHalfExtent(toDiameter);
        this.lerpStartNanos = System.nanoTime();
        this.lerpDurationNanos = durationMillis * 1_000_000L;
        this.halfExtent = lerpFromHalfExtent;
    }

    public boolean isActive() {
        return currentHalfExtent() < DEFAULT_HALF_EXTENT;
    }

    public double distanceToEdge(double x, double z) {
        double dx = Math.abs(x - centerX);
        double dz = Math.abs(z - centerZ);
        return currentHalfExtent() - Math.max(dx, dz);
    }

    public void reset() {
        centerX = 0.0;
        centerZ = 0.0;
        halfExtent = DEFAULT_HALF_EXTENT;
        lerpDurationNanos = 0;
    }

    private double currentHalfExtent() {
        if (lerpDurationNanos <= 0) return halfExtent;
        long elapsed = System.nanoTime() - lerpStartNanos;
        if (elapsed >= lerpDurationNanos) {
            halfExtent = lerpToHalfExtent;
            lerpDurationNanos = 0;
            return halfExtent;
        }
        double t = (double) elapsed / lerpDurationNanos;
        return lerpFromHalfExtent + (lerpToHalfExtent - lerpFromHalfExtent) * t;
    }

    private static double clampHalfExtent(double diameter) {
        return Math.max(1.0, diameter / 2.0);
    }
}
