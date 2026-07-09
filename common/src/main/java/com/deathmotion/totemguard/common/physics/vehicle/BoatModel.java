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

package com.deathmotion.totemguard.common.physics.vehicle;

import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.Getter;
import lombok.experimental.Accessors;

public final class BoatModel {

    public static final double BOAT_WIDTH = 1.375;
    public static final double BOAT_HEIGHT = 0.5625;

    private static final double GRAVITY = 0.04;
    private static final double UNDER_FLOWING_GRAVITY = 7.0E-4;
    private static final double UNDER_WATER_BUOYANCY = 0.01;
    private static final double BUOYANCY_SCALE = GRAVITY / 0.65;
    private static final double BOB_DRAG = 0.75;

    private static final double FRICTION_WATER = 0.9;
    private static final double FRICTION_UNDER_WATER = 0.45;
    private static final double FRICTION_AIR = 0.9;

    private static final double ACCEL_FORWARD = 0.04;
    private static final double ACCEL_BACKWARD = 0.005;
    private static final double ACCEL_PADDLE = 0.005;

    public enum Status {IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR}

    @Getter
    @Accessors(fluent = true)
    private Status status = Status.IN_AIR;
    private double waterLevel;
    private double landFriction;

    public void resolve(BlockReader reader, double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ) {
        if (isUnderwater(reader, minX, minY, minZ, maxX, maxY, maxZ)) {
            return;
        }
        if (checkInWater(reader, minX, minY, minZ, maxX, maxZ)) {
            status = Status.IN_WATER;
            return;
        }
        double friction = groundFriction(reader, minX, minY, minZ, maxX, maxZ);
        if (friction > 0.0) {
            landFriction = friction;
            status = Status.ON_LAND;
            return;
        }
        status = Status.IN_AIR;
    }

    public double advanceVertical(double vy, double boatY) {
        double gravityTerm = -GRAVITY;
        double buoyancy = 0.0;
        switch (status) {
            case IN_WATER -> buoyancy = (waterLevel - boatY) / BOAT_HEIGHT;
            case UNDER_FLOWING_WATER -> gravityTerm = -UNDER_FLOWING_GRAVITY;
            case UNDER_WATER -> buoyancy = UNDER_WATER_BUOYANCY;
            default -> {
            }
        }
        double advanced = vy + gravityTerm;
        if (buoyancy > 0.0) {
            advanced = (advanced + buoyancy * BUOYANCY_SCALE) * BOB_DRAG;
        }
        return advanced;
    }

    public double horizontalFriction(boolean playerControlled) {
        return switch (status) {
            case UNDER_WATER -> FRICTION_UNDER_WATER;
            case ON_LAND -> playerControlled ? landFriction / 2.0 : landFriction;
            default -> FRICTION_AIR;
        };
    }

    public double controlAccel() {
        return ACCEL_FORWARD + ACCEL_PADDLE;
    }

    public void reset() {
        status = Status.IN_AIR;
        waterLevel = 0.0;
        landFriction = 0.0;
    }

    private boolean isUnderwater(BlockReader reader, double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ) {
        double probeY = maxY + 0.001;
        int y = floor(probeY);
        boolean flowing = false;
        boolean anyWater = false;
        for (int x = floor(minX); x <= floor(maxX); x++) {
            for (int z = floor(minZ); z <= floor(maxZ); z++) {
                long facts = reader.facts(x, y, z);
                if (!StateFacts.is(facts, StateFacts.WATER)) continue;
                double height = StateFacts.is(reader.facts(x, y + 1, z), StateFacts.WATER)
                        ? 1.0
                        : StateFacts.fluidHeight(facts);
                if (probeY < y + height) {
                    anyWater = true;
                    if (!StateFacts.is(facts, StateFacts.FLUID_SOURCE)) flowing = true;
                }
            }
        }
        if (!anyWater) return false;
        waterLevel = maxY;
        status = flowing ? Status.UNDER_FLOWING_WATER : Status.UNDER_WATER;
        return true;
    }

    private boolean checkInWater(BlockReader reader, double minX, double minY, double minZ,
                                 double maxX, double maxZ) {
        int y = floor(minY);
        double level = Double.NEGATIVE_INFINITY;
        for (int x = floor(minX); x <= floor(maxX); x++) {
            for (int z = floor(minZ); z <= floor(maxZ); z++) {
                long facts = reader.facts(x, y, z);
                if (!StateFacts.is(facts, StateFacts.WATER)) continue;
                double height = StateFacts.is(reader.facts(x, y + 1, z), StateFacts.WATER)
                        ? 1.0
                        : StateFacts.fluidHeight(facts);
                level = Math.max(level, y + height);
            }
        }
        if (minY < level) {
            waterLevel = level;
            return true;
        }
        return false;
    }

    private double groundFriction(BlockReader reader, double minX, double minY, double minZ,
                                  double maxX, double maxZ) {
        double sum = 0.0;
        int count = 0;
        int y = floor(minY - 0.001);
        for (int x = floor(minX); x <= floor(maxX); x++) {
            for (int z = floor(minZ); z <= floor(maxZ); z++) {
                long facts = reader.facts(x, y, z);
                if (!StateFacts.is(facts, StateFacts.HAS_SHAPE)) continue;
                StateType type = reader.state(x, y, z).getType();
                if (type == StateTypes.LILY_PAD) continue;
                sum += StateFacts.slipperiness(facts);
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
