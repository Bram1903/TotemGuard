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
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.world.entity.EntityRoles;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.deathmotion.totemguard.common.world.entity.TrackedEntity;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public class OutboundEntityProcessor extends ProcessorOutbound {

    private static final int FISHING_ROD_PULL_STATUS = 31;
    private static final double FISHING_PULL_SCALE = 0.1;

    private final Data data;
    private final EntityTracker entities;
    private final PacketLatencyHandler latencyHandler;
    private int serverVehicleId = -1;

    public OutboundEntityProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.entities = player.getWorldMirror().entities();
        this.latencyHandler = player.getLatencyHandler();
    }

    private static double interpolationSpread(TrackedEntity entity) {
        double spread = Math.max(Math.abs(entity.targetX() - entity.renderX()),
                Math.abs(entity.renderX() - entity.prevRenderX()));
        spread = Math.max(spread, Math.max(Math.abs(entity.targetY() - entity.renderY()),
                Math.abs(entity.renderY() - entity.prevRenderY())));
        return Math.max(spread, Math.max(Math.abs(entity.targetZ() - entity.renderZ()),
                Math.abs(entity.renderZ() - entity.prevRenderZ())));
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
            if (packet.getEntityType() == EntityTypes.FISHING_BOBBER) {
                data.getFishingData().onHookSpawn(packet.getEntityId(), packet.getData());
            }
            spawn(event, packet.getEntityId(), packet.getEntityType(), packet.getPosition());
        } else if (type == PacketType.Play.Server.SPAWN_LIVING_ENTITY) {
            WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(event);
            spawn(event, packet.getEntityId(), packet.getEntityType(), packet.getPosition());
        } else if (type == PacketType.Play.Server.SPAWN_PLAYER) {
            WrapperPlayServerSpawnPlayer packet = new WrapperPlayServerSpawnPlayer(event);
            spawn(event, packet.getEntityId(), EntityTypes.PLAYER, packet.getPosition());
        } else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE) {
            WrapperPlayServerEntityRelativeMove packet = new WrapperPlayServerEntityRelativeMove(event);
            final int id = packet.getEntityId();
            final double dx = packet.getDeltaX(), dy = packet.getDeltaY(), dz = packet.getDeltaZ();
            latencyHandler.compensateLazy(event, () -> entities.nudge(id, dx, dy, dz));
        } else if (type == PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) {
            WrapperPlayServerEntityRelativeMoveAndRotation packet = new WrapperPlayServerEntityRelativeMoveAndRotation(event);
            final int id = packet.getEntityId();
            final double dx = packet.getDeltaX(), dy = packet.getDeltaY(), dz = packet.getDeltaZ();
            latencyHandler.compensateLazy(event, () -> entities.nudge(id, dx, dy, dz));
        } else if (type == PacketType.Play.Server.ENTITY_TELEPORT) {
            WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(event);
            place(event, packet.getEntityId(), packet.getPosition());
        } else if (type == PacketType.Play.Server.ENTITY_POSITION_SYNC) {
            WrapperPlayServerEntityPositionSync packet = new WrapperPlayServerEntityPositionSync(event);
            place(event, packet.getId(), packet.getValues().getPosition());
        } else if (type == PacketType.Play.Server.ENTITY_STATUS) {
            WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(event);
            if (packet.getStatus() == FISHING_ROD_PULL_STATUS
                    && data.getFishingData().isHook(packet.getEntityId())) {
                handleFishingPull(event, packet.getEntityId());
            }
        } else if (type == PacketType.Play.Server.DESTROY_ENTITIES) {
            handleDestroyEntities(event);
        } else if (type == PacketType.Play.Server.SET_PASSENGERS) {
            handleSetPassengers(event);
        } else if (type == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            handleEquipment(event);
        } else if (type == PacketType.Play.Server.RESPAWN) {
            serverVehicleId = -1;
            entities.clearAuthoritative();
        }
    }

    private void handleEquipment(PacketSendEvent event) {
        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);
        final int entityId = packet.getEntityId();
        EntityType entityType = entities.announcedType(entityId);
        boolean saddleable = EntityRoles.horseFamily(entityType) || EntityRoles.steerableMob(entityType);
        boolean ghast = EntityRoles.happyGhast(entityType);
        if (!saddleable && !ghast) return;
        for (Equipment equipment : packet.getEquipment()) {
            EquipmentSlot slot = equipment.getSlot();
            ItemStack item = equipment.getItem();
            final boolean present = item != null && !item.isEmpty();
            if (saddleable && slot == EquipmentSlot.SADDLE) {
                latencyHandler.compensate(event, () -> entities.setSaddled(entityId, present));
            } else if (ghast && slot == EquipmentSlot.BODY) {
                latencyHandler.compensate(event, () -> entities.setHarnessed(entityId, present));
            }
        }
    }

    private void handleFishingPull(PacketSendEvent event, int hookId) {
        latencyHandler.compensate(event, () -> {
            if (data.getFishingData().hookedOf(hookId) != player.getUser().getEntityId()) return;
            TrackedEntity hook = entities.resolve(hookId);
            TrackedEntity owner = entities.resolve(data.getFishingData().ownerOf(hookId));
            if (hook == null || owner == null || !hook.positioned() || !owner.positioned()) return;
            double px = (owner.renderX() - hook.renderX()) * FISHING_PULL_SCALE;
            double py = (owner.renderY() - hook.renderY()) * FISHING_PULL_SCALE;
            double pz = (owner.renderZ() - hook.renderZ()) * FISHING_PULL_SCALE;
            double slack = (interpolationSpread(hook) + interpolationSpread(owner)) * FISHING_PULL_SCALE;
            data.getExternalVelocityData().addPush(px, py, pz, slack);
        });
    }

    private void spawn(PacketSendEvent event, int entityId, EntityType entityType, Vector3d pos) {
        entities.announce(entityId, entityType);
        if (entityType == EntityTypes.FIREWORK_ROCKET) {
            data.getFireworkData().candidate(entityId);
        }
        final double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        latencyHandler.compensateLazy(event, () -> entities.spawn(entityId, entityType, x, y, z));
    }

    private void place(PacketSendEvent event, int entityId, Vector3d pos) {
        final double x = pos.getX(), y = pos.getY(), z = pos.getZ();
        latencyHandler.compensateLazy(event, () -> entities.place(entityId, x, y, z));
    }

    private void handleDestroyEntities(PacketSendEvent event) {
        WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(event);
        final int[] entityIds = packet.getEntityIds();
        for (int entityId : entityIds) {
            entities.retract(entityId);
        }
        latencyHandler.compensateLazy(event, () -> {
            for (int entityId : entityIds) {
                entities.destroy(entityId);
                data.getFireworkData().onRemove(entityId);
                data.getFishingData().onRemove(entityId);
            }
        });

        if (serverVehicleId == -1) return;
        for (int entityId : entityIds) {
            if (entityId == serverVehicleId) {
                serverVehicleId = -1;
                latencyHandler.compensate(event, () -> {
                    if (data.getVehicleId() == entityId) data.setVehicleId(-1);
                    data.getMovementData().markVehicleSwitchResync();
                    entities.clearAuthoritative();
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
        final boolean driverSeat = passengers.length > 0 && passengers[0] == playerId;

        final boolean wasInThisVehicle = serverVehicleId == vehicleId;

        if (playerInThisVehicle && !wasInThisVehicle) {
            serverVehicleId = vehicleId;
            latencyHandler.compensate(event, () -> {
                data.setVehicleId(vehicleId);
                data.getVehicleData().onMount();
                data.getVehicleData().setDriverSeat(driverSeat);
            });
        } else if (!playerInThisVehicle && wasInThisVehicle) {
            serverVehicleId = -1;
            latencyHandler.compensate(event, () -> {
                if (data.getVehicleId() == vehicleId) data.setVehicleId(-1);
                data.getMovementData().markVehicleSwitchResync();
                data.getVehicleData().onMount();
                data.getVehicleData().setDriverSeat(false);
                entities.clearAuthoritative();
            });
        } else if (playerInThisVehicle) {
            latencyHandler.compensate(event, () -> {
                if (data.getVehicleId() == vehicleId) {
                    data.getVehicleData().setDriverSeat(driverSeat);
                }
            });
        }
    }
}
