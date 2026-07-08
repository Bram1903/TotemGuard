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

package com.deathmotion.totemguard.common.physics.trace;

public final class TraceFrame {

    public static final int FLAG_SPRINT = 1;
    public static final int FLAG_SNEAK = 1 << 1;
    public static final int FLAG_JUMP_POSSIBLE = 1 << 2;
    public static final int FLAG_INVENTORY_OPEN = 1 << 3;
    public static final int FLAG_CLAIMED_GROUND = 1 << 4;
    public static final int FLAG_WALL_NEAR = 1 << 5;
    public static final int FLAG_START_OVERLAP = 1 << 6;
    public static final int FLAG_BUBBLE = 1 << 8;
    public static final int FLAG_STUCK = 1 << 9;
    public static final int FLAG_GROUNDED_END = 1 << 10;
    public static final int FLAG_ARRESTED = 1 << 11;
    public static final int FLAG_STEP_USED = 1 << 12;
    public static final int FLAG_ALT_CENTER = 1 << 13;

    public long tick;
    public double obsX, obsY, obsZ;
    public double yaw, pitch, prevYaw, prevPitch;
    public double preCarriedX, preCarriedZ, preCarriedFloor, preCarriedCeil;
    public double centerX, centerZ, radius, ceiling, floor;
    public double horizontalExcess, ascentExcess, descentExcess, phaseExcess;
    public byte outcome, reason, breach, medium, ground;
    public int flags;
    public double supportGap, ceilingClearance;
    public int reads, misses, uncertainHits;
    public double buffer, engineFall;
    public byte mitigation;

    public void copyFrom(TraceFrame other) {
        tick = other.tick;
        obsX = other.obsX;
        obsY = other.obsY;
        obsZ = other.obsZ;
        yaw = other.yaw;
        pitch = other.pitch;
        prevYaw = other.prevYaw;
        prevPitch = other.prevPitch;
        preCarriedX = other.preCarriedX;
        preCarriedZ = other.preCarriedZ;
        preCarriedFloor = other.preCarriedFloor;
        preCarriedCeil = other.preCarriedCeil;
        centerX = other.centerX;
        centerZ = other.centerZ;
        radius = other.radius;
        ceiling = other.ceiling;
        floor = other.floor;
        horizontalExcess = other.horizontalExcess;
        ascentExcess = other.ascentExcess;
        descentExcess = other.descentExcess;
        phaseExcess = other.phaseExcess;
        outcome = other.outcome;
        reason = other.reason;
        breach = other.breach;
        medium = other.medium;
        ground = other.ground;
        flags = other.flags;
        supportGap = other.supportGap;
        ceilingClearance = other.ceilingClearance;
        reads = other.reads;
        misses = other.misses;
        uncertainHits = other.uncertainHits;
        buffer = other.buffer;
        engineFall = other.engineFall;
        mitigation = other.mitigation;
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }
}
