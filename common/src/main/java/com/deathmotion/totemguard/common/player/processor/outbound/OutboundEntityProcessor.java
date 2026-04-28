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

package com.deathmotion.totemguard.common.player.processor.outbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public class OutboundEntityProcessor extends ProcessorOutbound {

    private final WorldEntityData worldEntityData;

    public OutboundEntityProcessor(TGPlayer player) {
        super(player);
        this.worldEntityData = player.getData().getWorldEntityData();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
            worldEntityData.add(packet.getEntityId(), packet.getEntityType());
        } else if (type == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(event);
            worldEntityData.add(packet.getEntityId(), packet.getEntityType());
        } else if (type == PacketType.Play.Server.SPAWN_PLAYER) {
            worldEntityData.add(new WrapperPlayServerSpawnPlayer(event).getEntityId(), EntityTypes.PLAYER);
        } else if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            for (int entityId : new WrapperPlayServerDestroyEntities(event).getEntityIds()) {
                worldEntityData.remove(entityId);
            }
        } else if (type == PacketType.Play.Server.UPDATE_ATTRIBUTES) {
            WrapperPlayServerUpdateAttributes packet = new WrapperPlayServerUpdateAttributes(event);
            int entityId = packet.getEntityId();
            if (!worldEntityData.isLoaded(entityId)) return;
            for (WrapperPlayServerUpdateAttributes.Property property : packet.getProperties()) {
                if (property.getAttribute() != Attributes.MAX_HEALTH) continue;
                worldEntityData.setMaxHealth(entityId, (float) property.calcValue());
                break;
            }
        } else if (type == PacketType.Play.Server.JOIN_GAME) {
            worldEntityData.handleJoinGame(new WrapperPlayServerJoinGame(event));
        } else if (type == PacketType.Play.Server.RESPAWN) {
            worldEntityData.handleRespawn(new WrapperPlayServerRespawn(event));
        } else if (type == PacketType.Play.Server.CONFIGURATION_START) {
            worldEntityData.handleConfigurationStart();
        }
    }
}
