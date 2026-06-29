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

package com.deathmotion.totemguard.common.player.movement;

import com.deathmotion.totemguard.common.player.data.*;
import com.github.retrooper.packetevents.protocol.world.Location;
import lombok.Getter;

public class MovementEstimator {

    private static final double FRICTION = MovementConstants.MAX_HORIZONTAL_FRICTION;
    private static final double ENVELOPE_PAD = 0.010;
    private static final double HIT_EPSILON = 0.002;
    private static final double FAST_MOVEMENT_CAP = 3.0;
    private static final double STOP_SPEED = 0.03;
    private static final double BORDER_MARGIN = 2.0;

    private static final int WINDOW = 20;
    private static final long WINDOW_MASK = (1L << WINDOW) - 1;
    private static final int HITS_FOR_MOVED = 5;
    private static final double STRONG_SINGLE_EXCESS = 0.40;

    private static final double ENTITY_PUSH_PER = 0.08;
    private static final double MAX_ENTITY_PUSH = 0.30;
    private static final double ENTITY_LAG_MARGIN = 0.5;

    private final Data data;

    private boolean initialized;
    private double prevVelX;
    private double prevVelZ;

    private long hitWindow;

    @Getter
    private MovementResult result = MovementResult.UNKNOWN;
    @Getter
    private double lastExcess;
    @Getter
    private boolean movedThisTick;

    public MovementEstimator(Data data) {
        this.data = data;
    }

    private static double horizontalExcess(Interval areaX, Interval areaZ, double obsVelX, double obsVelZ) {
        return Math.hypot(areaX.distanceOutside(obsVelX), areaZ.distanceOutside(obsVelZ));
    }

    public boolean movedHorizontally() {
        return result == MovementResult.MOVED;
    }

    public int windowHits() {
        return Long.bitCount(hitWindow);
    }

    public int hitsForMoved() {
        return HITS_FOR_MOVED;
    }

    public void onFlying() {
        final MovementData movement = data.getMovementData();
        final ExternalVelocityData external = data.getExternalVelocityData();

        final Location current = movement.getCurrent();
        final Location previous = movement.getPrevious();

        final double obsVelX = current.getX() - previous.getX();
        final double obsVelZ = current.getZ() - previous.getZ();

        try {
            if (!initialized) {
                anchor(obsVelX, obsVelZ);
                initialized = true;
                movedThisTick = false;
                result = MovementResult.UNKNOWN;
                return;
            }

            if (shouldBail(current, obsVelX, obsVelZ)) {
                anchor(obsVelX, obsVelZ);
                shiftWindow(false);
                movedThisTick = false;
                result = MovementResult.UNKNOWN;
                return;
            }

            if (!movement.isLastFlyingPositionChanged()) {
                anchor(0.0, 0.0);
                shiftWindow(false);
                movedThisTick = false;
                result = MovementResult.EXTERNAL;
                return;
            }

            Interval areaX = Interval.ZERO.hull(prevVelX * FRICTION).expand(ENVELOPE_PAD).add(external.x());
            Interval areaZ = Interval.ZERO.hull(prevVelZ * FRICTION).expand(ENVELOPE_PAD).add(external.z());

            double excess = horizontalExcess(areaX, areaZ, obsVelX, obsVelZ);

            if (excess > HIT_EPSILON) {
                double entityPush = nearbyEntityPush(current);
                if (entityPush > 0.0) {
                    excess = horizontalExcess(areaX.expand(entityPush), areaZ.expand(entityPush), obsVelX, obsVelZ);
                }
            }

            lastExcess = excess;
            movedThisTick = excess > HIT_EPSILON;

            if (!movedThisTick && Math.hypot(obsVelX, obsVelZ) < STOP_SPEED) {
                hitWindow = 0;
            } else {
                shiftWindow(movedThisTick);
            }

            int hits = Long.bitCount(hitWindow);
            if (excess >= STRONG_SINGLE_EXCESS || hits >= HITS_FOR_MOVED) {
                result = MovementResult.MOVED;
            } else if (movedThisTick) {
                result = MovementResult.UNKNOWN;
            } else {
                result = MovementResult.EXTERNAL;
            }

            anchor(obsVelX, obsVelZ);
        } finally {
            external.tick();
            data.getPistonData().tick();
        }
    }

    private boolean shouldBail(Location current, double obsVelX, double obsVelZ) {
        if (data.isInVehicle()) return true;
        if (data.isSwimming() || data.isGliding()) return true;
        if (data.getPistonData().isActive()) return true;
        if (Math.hypot(obsVelX, obsVelZ) > FAST_MOVEMENT_CAP) return true;
        WorldBorderData border = data.getWorldBorderData();
        return border.isActive() && border.distanceToEdge(current.getX(), current.getZ()) < BORDER_MARGIN;
    }

    private void shiftWindow(boolean hit) {
        hitWindow = ((hitWindow << 1) | (hit ? 1L : 0L)) & WINDOW_MASK;
    }

    private void anchor(double velX, double velZ) {
        prevVelX = velX;
        prevVelZ = velZ;
    }

    private double nearbyEntityPush(Location current) {
        PlayerAttributeData attributes = data.getAttributeData();
        int count = data.getWorldEntityData().countPushableNear(
                current.getX(), current.getY(), current.getZ(),
                attributes.width() / 2.0, attributes.height(), ENTITY_LAG_MARGIN);
        return Math.min(MAX_ENTITY_PUSH, count * ENTITY_PUSH_PER);
    }

    public void reset() {
        initialized = false;
        prevVelX = 0;
        prevVelZ = 0;
        clearHistory();
        data.getExternalVelocityData().reset();
        data.getPistonData().reset();
    }

    public void clearHistory() {
        hitWindow = 0;
        result = MovementResult.UNKNOWN;
        lastExcess = 0;
        movedThisTick = false;
    }
}
