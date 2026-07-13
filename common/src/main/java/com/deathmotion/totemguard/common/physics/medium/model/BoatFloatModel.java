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

package com.deathmotion.totemguard.common.physics.medium.model;

import com.deathmotion.totemguard.common.physics.collision.ColliderBuffer;
import com.deathmotion.totemguard.common.physics.collision.ColliderCollector;
import com.deathmotion.totemguard.common.world.block.BlockReader;
import com.deathmotion.totemguard.common.world.block.StateFacts;
import com.deathmotion.totemguard.common.world.shape.ShapeQuery;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import lombok.Getter;
import lombok.experimental.Accessors;

public final class BoatFloatModel {

    public static final double BOAT_WIDTH = 1.375;
    public static final double BOAT_HEIGHT = 0.5625;
    public static final double SNAP_RISE = 0.101;

    public static final double GRAVITY = 0.04;
    public static final double CONTROL_MIN = -0.005;
    public static final double CONTROL_MAX = 0.04;
    private static final double UNDER_FLOWING_GRAVITY = 7.0E-4;
    private static final double UNDER_WATER_BUOYANCY = 0.01;
    private static final double BUOYANCY_SCALE = GRAVITY / 0.65;
    private static final double BOB_DRAG = 0.75;
    private static final double FRICTION_WATER = 0.9;
    private static final double FRICTION_UNDER_WATER = 0.45;
    private static final double FRICTION_AIR = 0.9;
    private static final double FRICTION_SLAB = 0.001;
    private static final double SURFACE_PROBE = 0.001;
    private final ColliderBuffer frictionScratch = new ColliderBuffer();
    @Getter
    @Accessors(fluent = true)
    private Status status = Status.IN_AIR;
    @Getter
    @Accessors(fluent = true)
    private Status oldStatus = Status.IN_AIR;
    private boolean known;
    private boolean oldStatusKnown;
    private double waterLevel;
    private double landFriction;

    private static boolean watery(Status status) {
        return status == Status.IN_WATER || status == Status.UNDER_WATER
                || status == Status.UNDER_FLOWING_WATER;
    }

    private static float fluidHeight(BlockReader reader, long facts, int x, int y, int z) {
        return StateFacts.is(reader.facts(x, y + 1, z), StateFacts.WATER)
                ? 1.0F
                : StateFacts.fluidAmount(facts) / 9.0F;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }

    public void resolve(BlockReader reader, ShapeQuery query,
                        double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ) {
        Status previous = status;
        boolean hadStatus = known;
        status = resolveStatus(reader, query, minX, minY, minZ, maxX, maxY, maxZ);
        oldStatus = previous;
        oldStatusKnown = hadStatus;
        known = true;
    }

    public boolean snapEligible() {
        return watery(status) && (!oldStatusKnown || oldStatus == Status.IN_AIR);
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

    public double horizontalFriction() {
        return switch (status) {
            case UNDER_WATER -> FRICTION_UNDER_WATER;
            case ON_LAND -> landFriction;
            default -> FRICTION_AIR;
        };
    }

    public double waterLevelAbove(BlockReader reader,
                                  double minX, double minZ, double maxX, double maxY, double maxZ,
                                  double lastDy) {
        int x0 = floor(minX), x1 = ceil(maxX);
        int y0 = floor(maxY), y1 = ceil(maxY - lastDy);
        int z0 = floor(minZ), z1 = ceil(maxZ);
        layers:
        for (int y = y0; y < y1; y++) {
            float level = 0.0F;
            for (int x = x0; x < x1; x++) {
                for (int z = z0; z < z1; z++) {
                    long facts = reader.facts(x, y, z);
                    if (StateFacts.is(facts, StateFacts.WATER)) {
                        level = Math.max(level, fluidHeight(reader, facts, x, y, z));
                    }
                    if (level >= 1.0F) continue layers;
                }
            }
            return y + level;
        }
        return y1 + 1;
    }

    public void reset() {
        status = Status.IN_AIR;
        oldStatus = Status.IN_AIR;
        known = false;
        oldStatusKnown = false;
        waterLevel = 0.0;
        landFriction = 0.0;
    }

    private Status resolveStatus(BlockReader reader, ShapeQuery query,
                                 double minX, double minY, double minZ,
                                 double maxX, double maxY, double maxZ) {
        Status underwater = underwaterStatus(reader, minX, minZ, maxX, maxY, maxZ);
        if (underwater != null) {
            waterLevel = maxY;
            return underwater;
        }
        if (checkInWater(reader, minX, minY, minZ, maxX, maxZ)) {
            return Status.IN_WATER;
        }
        double friction = groundFriction(reader, query, minX, minY, minZ, maxX, maxZ);
        if (friction > 0.0) {
            landFriction = friction;
            return Status.ON_LAND;
        }
        return Status.IN_AIR;
    }

    private Status underwaterStatus(BlockReader reader,
                                    double minX, double minZ, double maxX, double maxY, double maxZ) {
        double probeY = maxY + SURFACE_PROBE;
        int x0 = floor(minX), x1 = ceil(maxX);
        int y0 = floor(maxY), y1 = ceil(probeY);
        int z0 = floor(minZ), z1 = ceil(maxZ);
        boolean under = false;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    long facts = reader.facts(x, y, z);
                    if (!StateFacts.is(facts, StateFacts.WATER)) continue;
                    if (probeY < y + fluidHeight(reader, facts, x, y, z)) {
                        if (!StateFacts.is(facts, StateFacts.FLUID_SOURCE)) {
                            return Status.UNDER_FLOWING_WATER;
                        }
                        under = true;
                    }
                }
            }
        }
        return under ? Status.UNDER_WATER : null;
    }

    private boolean checkInWater(BlockReader reader,
                                 double minX, double minY, double minZ,
                                 double maxX, double maxZ) {
        int x0 = floor(minX), x1 = ceil(maxX);
        int y0 = floor(minY), y1 = ceil(minY + SURFACE_PROBE);
        int z0 = floor(minZ), z1 = ceil(maxZ);
        boolean in = false;
        waterLevel = -Double.MAX_VALUE;
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    long facts = reader.facts(x, y, z);
                    if (!StateFacts.is(facts, StateFacts.WATER)) continue;
                    float surface = y + fluidHeight(reader, facts, x, y, z);
                    waterLevel = Math.max(surface, waterLevel);
                    in |= minY < surface;
                }
            }
        }
        return in;
    }

    private double groundFriction(BlockReader reader, ShapeQuery query,
                                  double minX, double minY, double minZ,
                                  double maxX, double maxZ) {
        double slabMinY = minY - FRICTION_SLAB;
        int x0 = floor(minX), x1 = ceil(maxX);
        int y0 = floor(slabMinY) - 1, y1 = ceil(minY);
        int z0 = floor(minZ), z1 = ceil(maxZ);
        float sum = 0.0F;
        int count = 0;
        for (int x = x0; x < x1; x++) {
            for (int z = z0; z < z1; z++) {
                for (int y = y0; y < y1; y++) {
                    int clientId = reader.stateId(x, y, z);
                    long facts = reader.factsForClientId(clientId);
                    if (!StateFacts.is(facts, StateFacts.HAS_SHAPE)) continue;
                    WrappedBlockState state = reader.stateForClientId(clientId);
                    if (state.getType() == StateTypes.LILY_PAD) continue;
                    if (!shapeIntersectsSlab(reader, query, clientId, facts, x, y, z,
                            minX, slabMinY, minZ, maxX, minY, maxZ)) {
                        continue;
                    }
                    sum += (float) StateFacts.slipperiness(facts);
                    count++;
                }
            }
        }
        return count == 0 ? 0.0 : sum / (float) count;
    }

    private boolean shapeIntersectsSlab(BlockReader reader, ShapeQuery query,
                                        int clientId, long facts, int x, int y, int z,
                                        double minX, double minY, double minZ,
                                        double maxX, double maxY, double maxZ) {
        frictionScratch.reset();
        frictionScratch.tag(facts | ColliderBuffer.KIND_BLOCK);
        ColliderCollector.collectShape(frictionScratch, reader, query, clientId, facts, x, y, z);
        int boxes = frictionScratch.count();
        for (int i = 0; i < boxes; i++) {
            if (frictionScratch.minX(i) < maxX && frictionScratch.maxX(i) > minX
                    && frictionScratch.minY(i) < maxY && frictionScratch.maxY(i) > minY
                    && frictionScratch.minZ(i) < maxZ && frictionScratch.maxZ(i) > minZ) {
                return true;
            }
        }
        return false;
    }

    public enum Status {IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR}
}
