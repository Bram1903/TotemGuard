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
    public static final int FLAG_SWIMMING = 1 << 14;
    public static final int FLAG_SWIM_STEER = 1 << 15;
    public static final int FLAG_CLIMBABLE = 1 << 16;
    public static final int FLAG_FLUID_HOP = 1 << 17;
    public static final int FLAG_GLIDE_CLAIM = 1 << 18;
    public static final int FLAG_GLIDE_RIPTIDE = 1 << 19;
    public static final int FLAG_GLIDE_EXIT = 1 << 20;
    public static final int FLAG_CLIMB_UNCERTAIN = 1 << 21;
    public static final int FLAG_EYE_IN_WATER = 1 << 22;
    public static final int FLAG_WATER_AT_FEET = 1 << 23;
    public static final int FLAG_RAW_SPRINT = 1 << 24;
    public static final int FLAG_ECHO_LANDED = 1 << 25;
    public static final int FLAG_ECHO_SPRINT = 1 << 26;
    public static final int FLAG_ECHO_SWIM = 1 << 27;

    public static final long WIDENED_KNOCKBACK = 1L;
    public static final long WIDENED_RIPTIDE = 1L << 1;
    public static final long WIDENED_PISTON = 1L << 2;
    public static final long WIDENED_ENTITY_PUSH = 1L << 3;
    public static final long WIDENED_STUCK = 1L << 4;
    public static final long WIDENED_BUBBLE = 1L << 5;
    public static final long WIDENED_BED_BOUNCE = 1L << 6;
    public static final long WIDENED_HONEY_SLIDE = 1L << 7;
    public static final long WIDENED_BOOST_SEGMENT = 1L << 8;
    public static final long WIDENED_STEP_CARRY = 1L << 9;
    public static final long WIDENED_SNEAK_EDGE = 1L << 10;
    public static final long WIDENED_RESIDUAL_CARRY = 1L << 11;
    public static final long SPAWN_STEP = 1L << 12;
    public static final long SPAWN_KNOCKBACK = 1L << 13;
    public static final long SPAWN_GLIDE_EXIT = 1L << 14;
    public static final long HYPOTHESIS_OVERFLOW = 1L << 15;
    public static final long SPAWN_VERTICAL_DUAL = 1L << 16;
    public static final long SPAWN_COLLIDE_ZERO = 1L << 17;
    public static final long INPUT_EXACT = 1L << 18;
    public static final long SPAWN_AIR_REGIME = 1L << 19;
    public static final long STEP_FROM_FALL = 1L << 20;
    public static final long FLY_TRANSITION = 1L << 21;
    public static final long PINNED_BOUNCE_RISE = 1L << 22;

    private static final String[] WIDENING_NAMES = {
            "knockback", "riptide", "piston", "push", "stuck", "bubble",
            "bed", "honey", "boost", "step", "sneak", "carry",
            "spawn-step", "spawn-kb", "spawn-glide", "hyp-overflow",
            "spawn-vdual", "spawn-collide", "input-exact", "spawn-air",
            "step-fall", "trans-hold", "bounce-rise"
    };
    public long tick;
    public byte stream;
    public byte body;
    public double obsX, obsY, obsZ;
    public double yaw, pitch, prevYaw, prevPitch;
    public double preCarriedX, preCarriedZ, preCarriedFloor, preCarriedCeil;
    public double centerX, centerZ, radius, ceiling, floor;
    public double altCenterX, altCenterZ;
    public boolean altPresent;
    public double horizontalExcess, ascentExcess, descentExcess, phaseExcess;
    public byte outcome, reason, breach, medium, ground;
    public byte chosenSlot, liveCount;
    public int flags;
    public long contributors;
    public double supportGap, ceilingClearance;
    public int reads, misses, uncertainHits;
    public double buffer, engineFall;
    public byte mitigation;
    public double pushX, pushY, pushZ, bubbleAscent;
    public double stuckHorizontal, stuckVertical;
    public double fluidFriction, fluidAccel;
    public double boxMinX, boxFeetY, boxMinZ, boxMaxX, boxHeadY, boxMaxZ, eyeSampleY;
    public boolean wetCellFound;
    public int wetCellX, wetCellY, wetCellZ;
    public double wetCellSurface;
    public int fluidCellX0, fluidCellX1, fluidCellY0, fluidCellY1, fluidCellZ0, fluidCellZ1;
    public double moveSpeed, jumpStrength, stepHeight;
    public double riptideStrength;
    public int fireworkMin, fireworkMax;

    public static String describeWidenings(long contributors) {
        if (contributors == 0L) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < WIDENING_NAMES.length; i++) {
            if ((contributors & (1L << i)) == 0L) continue;
            if (out.length() > 0) out.append('|');
            out.append(WIDENING_NAMES[i]);
        }
        return out.toString();
    }

    public void copyFrom(TraceFrame other) {
        tick = other.tick;
        stream = other.stream;
        body = other.body;
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
        altCenterX = other.altCenterX;
        altCenterZ = other.altCenterZ;
        altPresent = other.altPresent;
        horizontalExcess = other.horizontalExcess;
        ascentExcess = other.ascentExcess;
        descentExcess = other.descentExcess;
        phaseExcess = other.phaseExcess;
        outcome = other.outcome;
        reason = other.reason;
        breach = other.breach;
        medium = other.medium;
        ground = other.ground;
        chosenSlot = other.chosenSlot;
        liveCount = other.liveCount;
        flags = other.flags;
        contributors = other.contributors;
        supportGap = other.supportGap;
        ceilingClearance = other.ceilingClearance;
        reads = other.reads;
        misses = other.misses;
        uncertainHits = other.uncertainHits;
        buffer = other.buffer;
        engineFall = other.engineFall;
        mitigation = other.mitigation;
        pushX = other.pushX;
        pushY = other.pushY;
        pushZ = other.pushZ;
        bubbleAscent = other.bubbleAscent;
        stuckHorizontal = other.stuckHorizontal;
        stuckVertical = other.stuckVertical;
        fluidFriction = other.fluidFriction;
        fluidAccel = other.fluidAccel;
        boxMinX = other.boxMinX;
        boxFeetY = other.boxFeetY;
        boxMinZ = other.boxMinZ;
        boxMaxX = other.boxMaxX;
        boxHeadY = other.boxHeadY;
        boxMaxZ = other.boxMaxZ;
        eyeSampleY = other.eyeSampleY;
        wetCellFound = other.wetCellFound;
        wetCellX = other.wetCellX;
        wetCellY = other.wetCellY;
        wetCellZ = other.wetCellZ;
        wetCellSurface = other.wetCellSurface;
        fluidCellX0 = other.fluidCellX0;
        fluidCellX1 = other.fluidCellX1;
        fluidCellY0 = other.fluidCellY0;
        fluidCellY1 = other.fluidCellY1;
        fluidCellZ0 = other.fluidCellZ0;
        fluidCellZ1 = other.fluidCellZ1;
        moveSpeed = other.moveSpeed;
        jumpStrength = other.jumpStrength;
        stepHeight = other.stepHeight;
        riptideStrength = other.riptideStrength;
        fireworkMin = other.fireworkMin;
        fireworkMax = other.fireworkMax;
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }
}
