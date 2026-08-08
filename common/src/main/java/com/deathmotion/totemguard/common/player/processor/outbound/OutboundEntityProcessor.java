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
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public class OutboundEntityProcessor extends ProcessorOutbound {

    private final Data data;
    private final WorldEntityData worldEntityData;
    private final PacketLatencyHandler latencyHandler;
    private int serverVehicleId = -1;

    public OutboundEntityProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.worldEntityData = player.getData().getWorldEntityData();
        this.latencyHandler = player.getLatencyHandler();
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
            handleDestroyEntities(event);
        } else if (type == PacketType.Play.Server.SET_PASSENGERS) {
            handleSetPassengers(event);
        } else if (type == PacketType.Play.Server.JOIN_GAME) {
            worldEntityData.handleJoinGame(new WrapperPlayServerJoinGame(event));
        } else if (type == PacketType.Play.Server.RESPAWN) {
            worldEntityData.handleRespawn(new WrapperPlayServerRespawn(event));
            serverVehicleId = -1;
        } else if (type == PacketType.Play.Server.CONFIGURATION_START) {
            worldEntityData.handleConfigurationStart();
        }
    }

    private void handleDestroyEntities(PacketSendEvent event) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);
        int[] entityIds = packet.getEntityIds();
        for (int entityId : entityIds) {
            worldEntityData.remove(entityId);
        }

        if (serverVehicleId == -1) return;
        for (int entityId : entityIds) {
            if (entityId == serverVehicleId) {
                serverVehicleId = -1;
                latencyHandler.compensate(event, () -> {
                    if (data.getVehicleId() == entityId) data.setVehicleId(-1);
                    data.getMovementData().markVehicleSwitchResync();
                });
                return;
            }
        }
    }

    private void handleSetPassengers(PacketSendEvent event) {
        WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
        final int vehicleId = packet.getEntityId();
        final int playerId = player.getUser().getEntityId();
        final int[] passengers = packet.getPassengers();

        boolean playerInThisVehicle = false;
        for (int passenger : passengers) {
            if (passenger == playerId) {
                playerInThisVehicle = true;
                break;
            }
        }

        final boolean wasInThisVehicle = serverVehicleId == vehicleId;

        if (playerInThisVehicle && !wasInThisVehicle) {
            serverVehicleId = vehicleId;
            latencyHandler.compensate(event, () -> data.setVehicleId(vehicleId));
        } else if (!playerInThisVehicle && wasInThisVehicle) {
            serverVehicleId = -1;
            latencyHandler.compensate(event, () -> {
                if (data.getVehicleId() == vehicleId) data.setVehicleId(-1);
                data.getMovementData().markVehicleSwitchResync();
            });
        }
    }
}
