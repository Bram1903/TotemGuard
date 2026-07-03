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

import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.dimension.DimensionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WorldEntityData {

    private static final double SNAP_THRESHOLD = 4.0;

    private final Map<Integer, TrackedEntity> entities = new HashMap<>();

    @Getter
    private int standableCount;
    private String worldName;
    private DimensionType dimensionType;

    public Collection<TrackedEntity> tracked() {
        return entities.values();
    }

    public void add(int entityId, EntityType type, double x, double y, double z) {
        TrackedEntity tracked = new TrackedEntity(type);
        tracked.snapTo(x, y, z);
        TrackedEntity previous = entities.put(entityId, tracked);
        if (previous != null && previous.isStandable()) standableCount--;
        if (tracked.isStandable()) standableCount++;
    }

    public void remove(int entityId) {
        TrackedEntity removed = entities.remove(entityId);
        if (removed != null && removed.isStandable()) standableCount--;
    }

    public void setPosition(int entityId, double x, double y, double z) {
        TrackedEntity tracked = entities.get(entityId);
        if (tracked == null) return;
        if (!tracked.isPositioned()
                || Math.abs(x - tracked.getTargetX()) > SNAP_THRESHOLD
                || Math.abs(y - tracked.getTargetY()) > SNAP_THRESHOLD
                || Math.abs(z - tracked.getTargetZ()) > SNAP_THRESHOLD) {
            tracked.snapTo(x, y, z);
        } else {
            tracked.interpolateTo(x, y, z);
        }
    }

    public void move(int entityId, double dx, double dy, double dz) {
        TrackedEntity tracked = entities.get(entityId);
        if (tracked == null || !tracked.isPositioned()) return;
        tracked.addDelta(dx, dy, dz);
    }

    public void advanceInterpolation() {
        for (TrackedEntity tracked : entities.values()) {
            if (tracked.isPositioned()) tracked.advance();
        }
    }

    public void setScale(int entityId, double scale) {
        TrackedEntity tracked = entities.get(entityId);
        if (tracked == null) return;
        tracked.setScale(scale);
    }

    public void setSlimeSize(int entityId, int size) {
        TrackedEntity tracked = entities.get(entityId);
        if (tracked == null) return;
        tracked.setSlimeSize(size);
    }

    public boolean isLoaded(int entityId) {
        return entities.containsKey(entityId);
    }

    public boolean isPlayer(int entityId) {
        TrackedEntity tracked = entities.get(entityId);
        return tracked != null && tracked.getType() == EntityTypes.PLAYER;
    }

    public boolean isSlimeLike(int entityId) {
        TrackedEntity tracked = entities.get(entityId);
        return tracked != null && (tracked.getType() == EntityTypes.SLIME || tracked.getType() == EntityTypes.MAGMA_CUBE);
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
        entities.clear();
        standableCount = 0;
    }

    private boolean isWorldChange(WrapperPlayServerRespawn respawn) {
        if (respawn.getWorldName().isPresent()) {
            return !Objects.equals(respawn.getWorldName().orElse(null), worldName);
        }
        return !Objects.equals(respawn.getDimensionType(), dimensionType);
    }
}
