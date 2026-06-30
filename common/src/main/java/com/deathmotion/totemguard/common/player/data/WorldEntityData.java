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

package com.deathmotion.totemguard.common.player.data;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldEntityData {

    private static final double SNAP_THRESHOLD = 4.0;

    private final Map<Integer, Tracked> trackedEntities = new HashMap<>();
    private String worldName;
    private DimensionType dimensionType;

    private static boolean isPushable(EntityType type) {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
    }

    private static double intervalGap(double aMin, double aMax, double bMin, double bMax) {
        if (aMax < bMin) return bMin - aMax;
        if (bMax < aMin) return aMin - bMax;
        return 0.0;
    }

    public void add(int entityId, EntityType type, double x, double y, double z) {
        Tracked tracked = new Tracked(type);
        tracked.snapTo(x, y, z);
        trackedEntities.put(entityId, tracked);
    }

    public void remove(int entityId) {
        trackedEntities.remove(entityId);
    }

    public void setPosition(int entityId, double x, double y, double z) {
        Tracked tracked = trackedEntities.get(entityId);
        if (tracked == null) return;
        if (!tracked.positioned
                || Math.abs(x - tracked.targetX) > SNAP_THRESHOLD
                || Math.abs(y - tracked.targetY) > SNAP_THRESHOLD
                || Math.abs(z - tracked.targetZ) > SNAP_THRESHOLD) {
            tracked.snapTo(x, y, z);
        } else {
            tracked.interpolateTo(x, y, z);
        }
    }

    public void move(int entityId, double dx, double dy, double dz) {
        Tracked tracked = trackedEntities.get(entityId);
        if (tracked == null || !tracked.positioned) return;
        tracked.addDelta(dx, dy, dz);
    }

    public void advanceInterpolation() {
        for (Tracked tracked : trackedEntities.values()) {
            if (tracked.positioned) tracked.advance();
        }
    }

    public void setScale(int entityId, double scale) {
        Tracked tracked = trackedEntities.get(entityId);
        if (tracked == null) return;
        tracked.scale = Math.max(0.0625, Math.min(16.0, scale));
    }

    public void setSlimeSize(int entityId, int size) {
        Tracked tracked = trackedEntities.get(entityId);
        if (tracked == null) return;
        tracked.slimeSize = Math.max(1, Math.min(127, size));
    }

    public boolean isLoaded(int entityId) {
        return trackedEntities.containsKey(entityId);
    }

    public boolean isPlayer(int entityId) {
        Tracked tracked = trackedEntities.get(entityId);
        return tracked != null && tracked.type == EntityTypes.PLAYER;
    }

    public boolean isSlimeLike(int entityId) {
        Tracked tracked = trackedEntities.get(entityId);
        return tracked != null && (tracked.type == EntityTypes.SLIME || tracked.type == EntityTypes.MAGMA_CUBE);
    }

    public int countPushableNear(double pMinX, double pMinY, double pMinZ,
                                 double pMaxX, double pMaxY, double pMaxZ,
                                 double playerHalfWidth, double playerHeight) {
        int count = 0;
        for (Tracked tracked : trackedEntities.values()) {
            if (!tracked.positioned) continue;
            if (!tracked.pushable) continue;

            double eMinX = Math.min(tracked.prevRenderX, tracked.targetX);
            double eMaxX = Math.max(tracked.prevRenderX, tracked.targetX);
            double eMinY = Math.min(tracked.prevRenderY, tracked.targetY);
            double eMaxY = Math.max(tracked.prevRenderY, tracked.targetY);
            double eMinZ = Math.min(tracked.prevRenderZ, tracked.targetZ);
            double eMaxZ = Math.max(tracked.prevRenderZ, tracked.targetZ);

            double horizontalReach = playerHalfWidth + tracked.halfWidth();
            boolean xOk = intervalGap(pMinX, pMaxX, eMinX, eMaxX) < horizontalReach;
            boolean zOk = intervalGap(pMinZ, pMaxZ, eMinZ, eMaxZ) < horizontalReach;
            boolean yOk = pMinY < eMaxY + tracked.height() && eMinY < pMaxY + playerHeight;

            if (xOk && zOk && yOk) count++;
        }
        return count;
    }

    public void handleJoinGame(WrapperPlayServerJoinGame packet) {
        worldName = packet.getWorldName();
        dimensionType = packet.getDimensionType();
        clearAll();
    }

    public void handleRespawn(WrapperPlayServerRespawn packet) {
        if (isWorldChange(packet)) {
            clearAll();
        }
        worldName = packet.getWorldName().orElse(null);
        dimensionType = packet.getDimensionType();
    }

    public void handleConfigurationStart() {
        clearAll();
        worldName = null;
        dimensionType = null;
    }

    private void clearAll() {
        trackedEntities.clear();
    }

    private boolean isWorldChange(WrapperPlayServerRespawn respawn) {
        if (respawn.getWorldName().isPresent()) {
            return !Objects.equals(respawn.getWorldName().orElse(null), worldName);
        }
        return !Objects.equals(respawn.getDimensionType(), dimensionType);
    }

    private static final class Tracked {
        private static final int INTERPOLATION_STEPS = 3;

        private final EntityType type;
        private final boolean pushable;
        private final double baseHalfWidth;
        private final double baseHeight;
        private double scale = 1.0;
        private int slimeSize = 1;
        private boolean positioned;

        private double targetX;
        private double targetY;
        private double targetZ;
        private double renderX;
        private double renderY;
        private double renderZ;
        private double prevRenderX;
        private double prevRenderY;
        private double prevRenderZ;
        private int interpSteps;

        private Tracked(EntityType type) {
            this.type = type;
            this.pushable = isPushable(type);
            this.baseHalfWidth = EntityHitboxes.width(type) / 2.0;
            this.baseHeight = EntityHitboxes.height(type);
        }

        private void snapTo(double x, double y, double z) {
            targetX = renderX = prevRenderX = x;
            targetY = renderY = prevRenderY = y;
            targetZ = renderZ = prevRenderZ = z;
            interpSteps = 0;
            positioned = true;
        }

        private void interpolateTo(double x, double y, double z) {
            targetX = x;
            targetY = y;
            targetZ = z;
            interpSteps = INTERPOLATION_STEPS;
            positioned = true;
        }

        private void addDelta(double dx, double dy, double dz) {
            targetX += dx;
            targetY += dy;
            targetZ += dz;
            interpSteps = INTERPOLATION_STEPS;
        }

        private void advance() {
            prevRenderX = renderX;
            prevRenderY = renderY;
            prevRenderZ = renderZ;
            if (interpSteps > 0) {
                double t = 1.0 / interpSteps;
                renderX += (targetX - renderX) * t;
                renderY += (targetY - renderY) * t;
                renderZ += (targetZ - renderZ) * t;
                interpSteps--;
            }
        }

        private double halfWidth() {
            return baseHalfWidth * scale * slimeSize;
        }

        private double height() {
            return baseHeight * scale * slimeSize;
        }
    }
}
