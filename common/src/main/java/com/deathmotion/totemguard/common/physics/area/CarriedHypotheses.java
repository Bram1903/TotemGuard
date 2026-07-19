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

package com.deathmotion.totemguard.common.physics.area;

import com.deathmotion.totemguard.common.util.ClientMath;

import java.util.function.UnaryOperator;

public final class CarriedHypotheses {

    public static final int CAPACITY = 4;
    private static final double CONVERGENCE_EPS = 1.0e-9;
    private final MotionArea[] areas = new MotionArea[CAPACITY];
    private final Kind[] kinds = new Kind[CAPACITY];
    private final boolean[] live = new boolean[CAPACITY];
    private boolean overflowed;

    public CarriedHypotheses() {
        collapse(MotionArea.rest());
    }

    private static double pairDistance(MotionArea a, MotionArea b) {
        double centers = ClientMath.horizontalDistance(a.centerX() - b.centerX(), a.centerZ() - b.centerZ());
        double verticalGap = Math.max(0.0,
                Math.max(a.floorVy(), b.floorVy()) - Math.min(a.ceilVy(), b.ceilVy()));
        return centers + verticalGap;
    }

    private static MotionArea enclose(MotionArea a, MotionArea b) {
        double midX = (a.centerX() + b.centerX()) * 0.5;
        double midZ = (a.centerZ() + b.centerZ()) * 0.5;
        double halfDistance = ClientMath.horizontalDistance(
                a.centerX() - b.centerX(), a.centerZ() - b.centerZ()) * 0.5;
        return new MotionArea(midX, midZ,
                halfDistance + Math.max(a.slack(), b.slack()),
                Math.min(a.floorVy(), b.floorVy()),
                Math.max(a.ceilVy(), b.ceilVy()));
    }

    private static boolean agrees(MotionArea a, MotionArea b) {
        return Math.abs(a.centerX() - b.centerX()) <= CONVERGENCE_EPS
                && Math.abs(a.centerZ() - b.centerZ()) <= CONVERGENCE_EPS
                && Math.abs(a.slack() - b.slack()) <= CONVERGENCE_EPS
                && Math.abs(a.floorVy() - b.floorVy()) <= CONVERGENCE_EPS
                && Math.abs(a.ceilVy() - b.ceilVy()) <= CONVERGENCE_EPS;
    }

    public void collapse(MotionArea main) {
        areas[0] = main;
        kinds[0] = Kind.MAIN;
        live[0] = true;
        for (int slot = 1; slot < CAPACITY; slot++) {
            live[slot] = false;
            areas[slot] = null;
        }
    }

    public void mapInPlace(UnaryOperator<MotionArea> transform) {
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (live[slot]) areas[slot] = transform.apply(areas[slot]);
        }
    }

    public int liveCount() {
        int count = 0;
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (live[slot]) count++;
        }
        return count;
    }

    public boolean live(int slot) {
        return live[slot];
    }

    public MotionArea area(int slot) {
        return areas[slot];
    }

    public void area(int slot, MotionArea advanced) {
        areas[slot] = advanced;
    }

    public Kind kind(int slot) {
        return kinds[slot];
    }

    public void killKind(Kind kind) {
        for (int slot = 1; slot < CAPACITY; slot++) {
            if (live[slot] && kinds[slot] == kind) {
                live[slot] = false;
                areas[slot] = null;
            }
        }
    }

    public int spawn(Kind kind, MotionArea area) {
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (live[slot] && kinds[slot] == kind && agrees(areas[slot], area)) return slot;
        }
        int free = freeSlot();
        if (free < 0) {
            mergeNearestPair();
            overflowed = true;
            free = freeSlot();
        }
        live[free] = true;
        kinds[free] = kind;
        areas[free] = area;
        return free;
    }

    public void mergeConverged() {
        for (int low = 0; low < CAPACITY - 1; low++) {
            if (!live[low]) continue;
            for (int high = low + 1; high < CAPACITY; high++) {
                if (!live[high]) continue;
                if (agrees(areas[low], areas[high])) {
                    live[high] = false;
                    areas[high] = null;
                }
            }
        }
    }

    public MotionArea union() {
        return union(null);
    }

    public MotionArea union(Kind excluded) {
        MotionArea folded = null;
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (!live[slot]) continue;
            if (excluded != null && kinds[slot] == excluded) continue;
            folded = folded == null ? areas[slot] : enclose(folded, areas[slot]);
        }
        return folded == null ? areas[0] : folded;
    }

    public double minFloorVy() {
        double floor = Double.MAX_VALUE;
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (live[slot] && areas[slot].floorVy() < floor) floor = areas[slot].floorVy();
        }
        return floor == Double.MAX_VALUE ? 0.0 : floor;
    }

    public boolean pollOverflowed() {
        boolean result = overflowed;
        overflowed = false;
        return result;
    }

    private int freeSlot() {
        for (int slot = 0; slot < CAPACITY; slot++) {
            if (!live[slot]) return slot;
        }
        return -1;
    }

    private void mergeNearestPair() {
        int bestLow = 0;
        int bestHigh = 1;
        double best = Double.MAX_VALUE;
        for (int low = 0; low < CAPACITY - 1; low++) {
            for (int high = low + 1; high < CAPACITY; high++) {
                double distance = pairDistance(areas[low], areas[high]);
                if (distance < best) {
                    best = distance;
                    bestLow = low;
                    bestHigh = high;
                }
            }
        }
        areas[bestLow] = enclose(areas[bestLow], areas[bestHigh]);
        live[bestHigh] = false;
        areas[bestHigh] = null;
    }

    public enum Kind {
        MAIN,
        STEP_TRACK,
        KNOCKBACK_SET,
        GLIDE_EXIT,
        AIR_REGIME,
        SPARE
    }
}
