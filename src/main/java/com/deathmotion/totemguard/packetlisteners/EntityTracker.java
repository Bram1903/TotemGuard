/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.packetlisteners;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.manager.EntityCacheManager;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.*;

/**
 * Listens for EntityState events and manages the caching of various entity state details.
 */
public class EntityTracker implements PacketListener {
    private final EntityCacheManager cacheManager;

    /**
     * Constructs a new EntityState with the specified {@link TotemGuard}.
     *
     * @param plugin The platform to use.
     */
    public EntityTracker(TotemGuard plugin) {
        this.cacheManager = plugin.getEntityCacheManager();
    }

    /**
     * This function is called when an {@link PacketSendEvent} is triggered.
     * Manages the state of various entities based on the event triggered.
     *
     * @param event The event that has been triggered.
     */
    @Override
    public void onPacketSend(PacketSendEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (PacketType.Play.Server.SPAWN_LIVING_ENTITY == type) {
            handleSpawnLivingEntity(new WrapperPlayServerSpawnLivingEntity(event), event.getUser());
        } else if (PacketType.Play.Server.SPAWN_ENTITY == type) {
            handleSpawnEntity(new WrapperPlayServerSpawnEntity(event), event.getUser());
        } else if (PacketType.Play.Server.DESTROY_ENTITIES == type) {
            handleDestroyEntities(new WrapperPlayServerDestroyEntities(event), event.getUser());
        } else if (PacketType.Play.Server.RESPAWN == type) {
            handleRespawn(event.getUser());
        } else if (PacketType.Play.Server.JOIN_GAME == type) {
            handleJoinGame(event.getUser());
        } else if (PacketType.Play.Server.CONFIGURATION_START == type) {
            handleConfigurationStart(event.getUser());
        }
    }

    private void handleSpawnLivingEntity(WrapperPlayServerSpawnLivingEntity packet, User user) {
        EntityType entityType = packet.getEntityType();
        if (!EntityTypes.isTypeInstanceOf(entityType, EntityTypes.END_CRYSTAL)) return;

        int entityId = packet.getEntityId();
        cacheManager.addLivingEntity(user.getUUID(), entityId, entityType);
    }

    private void handleSpawnEntity(WrapperPlayServerSpawnEntity packet, User user) {
        EntityType entityType = packet.getEntityType();
        if (!EntityTypes.isTypeInstanceOf(entityType, EntityTypes.END_CRYSTAL)) return;

        int entityId = packet.getEntityId();
        cacheManager.addLivingEntity(user.getUUID(), entityId, entityType);
    }

    private void handleDestroyEntities(WrapperPlayServerDestroyEntities packet, User user) {
        for (int entityId : packet.getEntityIds()) {
            cacheManager.removeEntity(user.getUUID(), entityId);
        }
    }

    private void handleRespawn(User user) {
        cacheManager.resetUserCache(user.getUUID());
    }

    private void handleJoinGame(User user) {
        cacheManager.resetUserCache(user.getUUID());
    }

    private void handleConfigurationStart(User user) {
        cacheManager.resetUserCache(user.getUUID());
    }
}
