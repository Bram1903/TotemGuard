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
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;

@Accessors(fluent = true)
public final class EntityTracker {

    private static final int LEGACY_BOAT_INTERPOLATION_STEPS = 10;

    private static final double SNAP_THRESHOLD = 4.0;

    private final int boatInterpolationSteps;
    private final Map<Integer, TrackedEntity> entities = new HashMap<>();
    // Identity follows packet SEND order, positions follow acks: a CAMERA or METADATA sent right
    // after a SPAWN is valid before either is acked.
    private final Map<Integer, EntityType> announced = new HashMap<>();
    private final List<TrackedEntity> settling = new ArrayList<>();
    private int authoritativeId = -1;
    @Getter
    private int standableCount;
    @Getter
    private int pushableCount;

    public EntityTracker(ClientVersion clientVersion) {
        this.boatInterpolationSteps = clientVersion.isOlderThan(ClientVersion.V_1_21_2)
                ? LEGACY_BOAT_INTERPOLATION_STEPS
                : TrackedEntity.INTERPOLATION_STEPS;
    }

    private static double intervalGap(double aMin, double aMax, double bMin, double bMax) {
        if (aMax < bMin) return bMin - aMax;
        if (bMax < aMin) return aMin - bMax;
        return 0.0;
    }

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
        TrackedEntity entity = new TrackedEntity(type,
                EntityRoles.boat(type) ? boatInterpolationSteps : TrackedEntity.INTERPOLATION_STEPS);
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
        if (entityId == authoritativeId) authoritativeId = -1;
    }

    public void setAuthoritative(int entityId, boolean authoritative) {
        if (authoritative) {
            authoritativeId = entityId;
        } else if (authoritativeId == entityId) {
            authoritativeId = -1;
        }
    }

    public void clearAuthoritative() {
        authoritativeId = -1;
    }

    public int authoritativeId() {
        return authoritativeId;
    }

    public void reconcileAuthority(int currentVehicleId) {
        if (authoritativeId >= 0 && authoritativeId != currentVehicleId) authoritativeId = -1;
    }

    public void driveAuthoritative(int entityId, double x, double y, double z) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.driveAuthoritative(x, y, z);
    }

    public void place(int entityId, double x, double y, double z) {
        if (entityId == authoritativeId) return;
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
        if (entityId == authoritativeId) return;
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

    public void setMovementSpeed(int entityId, double value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.movementSpeed(value);
    }

    public void setStepHeight(int entityId, double value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.stepHeight(value);
    }

    public void setBoostTime(int entityId, int total) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.startBoost(total);
    }

    public void setSuffocating(int entityId, boolean value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.suffocating(value);
    }

    public void setSaddled(int entityId, boolean value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.saddled(value);
    }

    public void setHarnessed(int entityId, boolean value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.harnessed(value);
    }

    public void setStaysStill(int entityId, boolean value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.staysStill(value);
    }

    public void setJumpStrength(int entityId, double value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.jumpStrength(value);
    }

    public void setGravity(int entityId, double value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.gravity(value);
    }

    public void setFlyingSpeed(int entityId, double value) {
        TrackedEntity entity = entities.get(entityId);
        if (entity == null) return;
        entity.flyingSpeed(value);
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
        authoritativeId = -1;
    }

    private void enqueueSettling(TrackedEntity entity) {
        if (entity.queuedForAdvance()) return;
        entity.queuedForAdvance(true);
        settling.add(entity);
    }

    public String describeStandablesNear(double x, double z, double radius) {
        StringBuilder out = new StringBuilder();
        int shown = 0;
        for (Map.Entry<Integer, TrackedEntity> entry : entities.entrySet()) {
            TrackedEntity entity = entry.getValue();
            if (!entity.positioned() || !entity.standable()) continue;
            double half = entity.halfWidth();
            if (Math.abs(entity.renderX() - x) > radius + half) continue;
            if (Math.abs(entity.renderZ() - z) > radius + half) continue;
            if (out.length() > 0) out.append(' ');
            out.append(String.format(java.util.Locale.ROOT, "#%d(%.3f,%.3f)tgt(%.3f,%.3f)stp%d top%.4f",
                    entry.getKey(), entity.renderX(), entity.renderZ(),
                    entity.targetX(), entity.targetZ(), entity.interpSteps(),
                    entity.spanMaxY() + entity.height()));
            if (++shown >= 8) break;
        }
        return out.length() == 0 ? "none" : out.toString();
    }

    public boolean isTracked(int entityId) {
        return announced.containsKey(entityId);
    }

    public boolean isPlayer(int entityId) {
        return announced.get(entityId) == EntityTypes.PLAYER;
    }

    public EntityType announcedType(int entityId) {
        return announced.get(entityId);
    }

    public boolean isSlimeLike(int entityId) {
        EntityType type = announced.get(entityId);
        return type == EntityTypes.SLIME || type == EntityTypes.MAGMA_CUBE;
    }

    public TrackedEntity nearestStandable(double x, double y, double z) {
        TrackedEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (TrackedEntity entity : entities.values()) {
            if (!entity.positioned() || !entity.standable()) continue;
            double dx = entity.renderX() - x;
            double dy = entity.renderY() - y;
            double dz = entity.renderZ() - z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    public int collectStandable(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ, ShapeSink sink, int excludeEntityId) {
        if (standableCount == 0) return 0;
        int emitted = 0;
        for (Map.Entry<Integer, TrackedEntity> entry : entities.entrySet()) {
            if (entry.getKey() == excludeEntityId) continue;
            TrackedEntity entity = entry.getValue();
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
}
