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

    private final Map<Integer, Tracked> trackedEntities = new HashMap<>();
    private String worldName;
    private DimensionType dimensionType;

    private static boolean isPushable(EntityType type) {
        return EntityTypes.isTypeInstanceOf(type, EntityTypes.LIVINGENTITY)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.BOAT)
                || EntityTypes.isTypeInstanceOf(type, EntityTypes.MINECART_ABSTRACT);
    }

    public void add(int entityId, EntityType type, double x, double y, double z) {
        Tracked tracked = new Tracked(type);
        tracked.x = x;
        tracked.y = y;
        tracked.z = z;
        tracked.positioned = true;
        trackedEntities.put(entityId, tracked);
    }

    public void remove(int entityId) {
        trackedEntities.remove(entityId);
    }

    public void setPosition(int entityId, double x, double y, double z) {
        Tracked tracked = trackedEntities.get(entityId);
        if (tracked == null) return;
        tracked.x = x;
        tracked.y = y;
        tracked.z = z;
        tracked.positioned = true;
    }

    public void move(int entityId, double dx, double dy, double dz) {
        Tracked tracked = trackedEntities.get(entityId);
        if (tracked == null || !tracked.positioned) return;
        tracked.x += dx;
        tracked.y += dy;
        tracked.z += dz;
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

    public int countPushableNear(double px, double py, double pz,
                                 double playerHalfWidth, double playerHeight, double lagMargin) {
        int count = 0;
        for (Tracked tracked : trackedEntities.values()) {
            if (!tracked.positioned) continue;
            if (!tracked.pushable) continue;

            double horizontalReach = playerHalfWidth + tracked.halfWidth() + lagMargin;
            if (Math.abs(tracked.x - px) > horizontalReach || Math.abs(tracked.z - pz) > horizontalReach) {
                continue;
            }
            if (py < tracked.y + tracked.height() + lagMargin && tracked.y < py + playerHeight + lagMargin) {
                count++;
            }
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
        private final EntityType type;
        private final boolean pushable;
        private final double baseHalfWidth;
        private final double baseHeight;
        private double scale = 1.0;
        private int slimeSize = 1;
        private double x;
        private double y;
        private double z;
        private boolean positioned;

        private Tracked(EntityType type) {
            this.type = type;
            this.pushable = isPushable(type);
            this.baseHalfWidth = EntityHitboxes.width(type) / 2.0;
            this.baseHeight = EntityHitboxes.height(type);
        }

        private double halfWidth() {
            return baseHalfWidth * scale * slimeSize;
        }

        private double height() {
            return baseHeight * scale * slimeSize;
        }
    }
}
