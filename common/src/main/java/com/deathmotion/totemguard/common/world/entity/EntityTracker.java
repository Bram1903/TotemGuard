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

package com.deathmotion.totemguard.common.world.entity;

import com.deathmotion.totemguard.common.world.shape.ShapeSink;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Accessors(fluent = true)
public final class EntityTracker {

    private static final double SNAP_THRESHOLD = 4.0;
    private static final double SUPPORT_HORIZONTAL_PAD = 0.1;
    private static final double SUPPORT_TOP_EPS = 0.03;
    private static final double SUPPORT_MAX_DROP = 2.0;

    private final Map<Integer, TrackedEntity> entities = new HashMap<>();
    // Identity follows packet SEND order, positions follow acks: a CAMERA or METADATA sent right
    // after a SPAWN is valid before either is acked.
    private final Map<Integer, EntityType> announced = new HashMap<>();
    private final List<TrackedEntity> settling = new ArrayList<>();

    @Getter
    private int standableCount;
    @Getter
    private int pushableCount;

    public Collection<TrackedEntity> tracked() {
        return entities.values();
    }

    public TrackedEntity resolve(int entityId) {
        return entities.get(entityId);
    }

    public void announce(int entityId, EntityType type) {
        announced.put(entityId, type);
    }

    public void retract(int entityId) {
        announced.remove(entityId);
    }

    public void spawn(int entityId, EntityType type, double x, double y, double z) {
        TrackedEntity entity = new TrackedEntity(type);
        entity.snapTo(x, y, z);
        TrackedEntity previous = entities.put(entityId, entity);
        if (previous != null) {
            if (previous.standable()) standableCount--;
            if (previous.pushable()) pushableCount--;
        }
        if (entity.standable()) standableCount++;
        if (entity.pushable()) pushableCount++;
    }

    public void destroy(int entityId) {
        TrackedEntity removed = entities.remove(entityId);
        if (removed != null) {
            if (removed.standable()) standableCount--;
            if (removed.pushable()) pushableCount--;
        }
    }

    public void place(int entityId, double x, double y, double z) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        if (!entity.positioned()
                || Math.abs(x - entity.targetX()) > SNAP_THRESHOLD
                || Math.abs(y - entity.targetY()) > SNAP_THRESHOLD
                || Math.abs(z - entity.targetZ()) > SNAP_THRESHOLD) {
            entity.snapTo(x, y, z);
        } else {
            entity.interpolateTo(x, y, z);
            enqueueSettling(entity);
        }
    }

    public void nudge(int entityId, double dx, double dy, double dz) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null || !entity.positioned()) return;
        entity.addDelta(dx, dy, dz);
        enqueueSettling(entity);
    }

    public void setScale(int entityId, double scale) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.scale(scale);
    }

    public void setSlimeSize(int entityId, int size) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.slimeSize(size);
    }

    public void advance() {
        for (int i = settling.size() - 1; i >= 0; i--) {
            TrackedEntity entity = settling.get(i);
            if (entity.advance()) {
                entity.queuedForAdvance(false);
                int last = settling.size() - 1;
                settling.set(i, settling.get(last));
                settling.remove(last);
            }
        }
    }

    public void clear() {
        entities.clear();
        announced.clear();
        settling.clear();
        standableCount = 0;
        pushableCount = 0;
    }

    private void enqueueSettling(TrackedEntity entity) {
        if (entity.queuedForAdvance()) return;
        entity.queuedForAdvance(true);
        settling.add(entity);
    }

    public boolean isTracked(int entityId) {
        return announced.containsKey(entityId);
    }

    public boolean isPlayer(int entityId) {
        return announced.get(entityId) == EntityTypes.PLAYER;
    }

    public boolean isSlimeLike(int entityId) {
        EntityType type = announced.get(entityId);
        return type == EntityTypes.SLIME || type == EntityTypes.MAGMA_CUBE;
    }

    public int collectStandable(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, ShapeSink sink) {
        if (standableCount == 0) return 0;
        int emitted = 0;
        for (TrackedEntity entity : entities.values()) {
            if (!entity.positioned() || !entity.standable()) continue;
            double half = entity.halfWidth();
            double eMinX = entity.spanMinX() - half;
            double eMaxX = entity.spanMaxX() + half;
            if (maxX <= eMinX || minX >= eMaxX) continue;
            double eMinZ = entity.spanMinZ() - half;
            double eMaxZ = entity.spanMaxZ() + half;
            if (maxZ <= eMinZ || minZ >= eMaxZ) continue;
            double eMinY = entity.spanMinY();
            double eMaxY = entity.spanMaxY() + entity.height();
            if (maxY <= eMinY || minY >= eMaxY) continue;
            sink.accept(eMinX, eMinY, eMinZ, eMaxX, eMaxY, eMaxZ);
            emitted++;
        }
        return emitted;
    }

    public double highestStandableTop(double pMinX, double pMinZ, double pMaxX, double pMaxZ, double feetY) {
        if (standableCount == 0) return Double.NEGATIVE_INFINITY;
        double best = Double.NEGATIVE_INFINITY;
        for (TrackedEntity entity : entities.values()) {
            if (!entity.positioned() || !entity.standable()) continue;

            double half = entity.halfWidth() + SUPPORT_HORIZONTAL_PAD;
            double eMinX = entity.spanMinX() - half;
            double eMaxX = entity.spanMaxX() + half;
            if (pMaxX <= eMinX || pMinX >= eMaxX) continue;
            double eMinZ = entity.spanMinZ() - half;
            double eMaxZ = entity.spanMaxZ() + half;
            if (pMaxZ <= eMinZ || pMinZ >= eMaxZ) continue;

            double top = entity.spanMaxY() + entity.height();
            if (top > feetY + SUPPORT_TOP_EPS) continue;
            if (top < feetY - SUPPORT_MAX_DROP) continue;
            if (top > best) best = top;
        }
        return best;
    }

    public int countPushableNear(double pMinX, double pMinY, double pMinZ,
                                 double pMaxX, double pMaxY, double pMaxZ,
                                 double playerHalfWidth, double playerHeight) {
        if (pushableCount == 0) return 0;
        int count = 0;
        for (TrackedEntity entity : entities.values()) {
            if (!entity.positioned() || !entity.pushable()) continue;

            double horizontalReach = playerHalfWidth + entity.halfWidth();
            boolean xOk = intervalGap(pMinX, pMaxX, entity.spanMinX(), entity.spanMaxX()) < horizontalReach;
            boolean zOk = intervalGap(pMinZ, pMaxZ, entity.spanMinZ(), entity.spanMaxZ()) < horizontalReach;
            boolean yOk = pMinY < entity.spanMaxY() + entity.height() && entity.spanMinY() < pMaxY + playerHeight;

            if (xOk && zOk && yOk) count++;
        }
        return count;
    }

    private static double intervalGap(double aMin, double aMax, double bMin, double bMax) {
        if (aMax < bMin) return bMin - aMax;
        if (bMax < aMin) return aMin - bMax;
        return 0.0;
    }
}
