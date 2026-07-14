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

    private final Map<Integer, EntityType> trackedEntities = new HashMap<>();

    private String worldName;
    private DimensionType dimensionType;

    public void add(int entityId, EntityType type) {
        trackedEntities.put(entityId, type);
    }

    public void remove(int entityId) {
        trackedEntities.remove(entityId);
    }

    public boolean isLoaded(int entityId) {
        return trackedEntities.containsKey(entityId);
    }

    public boolean isPlayer(int entityId) {
        return trackedEntities.get(entityId) == EntityTypes.PLAYER;
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
}
