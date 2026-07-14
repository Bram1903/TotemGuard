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
import com.deathmotion.totemguard.common.player.data.PlayerAttributeData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.world.entity.EntityTracker;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;

import java.util.List;
import java.util.UUID;

public class OutboundAttributeProcessor extends ProcessorOutbound {

    private static final UUID LEGACY_SPRINT_MODIFIER_ID = UUID.fromString("662a6b8d-da3e-4c1c-8813-96ea6097278d");
    private static final UUID LEGACY_FROST_MODIFIER_ID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");

    private final PlayerAttributeData attributes;
    private final EntityTracker entities;
    private final PacketLatencyHandler latencyHandler;

    public OutboundAttributeProcessor(TGPlayer player) {
        super(player);
        this.attributes = player.getData().getAttributeData();
        this.entities = player.getWorldMirror().entities();
        this.latencyHandler = player.getLatencyHandler();
    }

    private static double movementSpeedWithoutClientTransients(WrapperPlayServerUpdateAttributes.Property property) {
        List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = property.getModifiers();
        if (modifiers.stream().noneMatch(OutboundAttributeProcessor::isClientTransientModifier)) {
            return property.calcValue();
        }
        List<WrapperPlayServerUpdateAttributes.PropertyModifier> stripped = modifiers.stream()
                .filter(modifier -> !isClientTransientModifier(modifier))
                .toList();
        return new WrapperPlayServerUpdateAttributes.Property(property.getAttribute(), property.getValue(), stripped)
                .calcValue();
    }

    private static boolean isClientTransientModifier(WrapperPlayServerUpdateAttributes.PropertyModifier modifier) {
        ResourceLocation name = modifier.getName();
        if (name != null && "minecraft".equals(name.getNamespace())
                && ("sprinting".equals(name.getKey()) || "powder_snow".equals(name.getKey()))) {
            return true;
        }
        return LEGACY_SPRINT_MODIFIER_ID.equals(modifier.getUUID())
                || LEGACY_FROST_MODIFIER_ID.equals(modifier.getUUID());
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.UPDATE_ATTRIBUTES) return;

        WrapperPlayServerUpdateAttributes packet = new WrapperPlayServerUpdateAttributes(event);
        int entityId = packet.getEntityId();
        boolean self = entityId == player.getUser().getEntityId();

        for (WrapperPlayServerUpdateAttributes.Property property : packet.getProperties()) {
            if (property.getAttribute() == Attributes.SCALE) {
                if (!clientObserves(ClientVersion.V_1_20_5)) continue;
                double value = property.calcValue();
                if (self) {
                    latencyHandler.compensate(event, () -> attributes.setScale(value));
                } else {
                    latencyHandler.compensateLazy(event, () -> entities.setScale(entityId, value));
                }
            } else if (property.getAttribute() == Attributes.MOVEMENT_SPEED) {
                if (self) {
                    double value = movementSpeedWithoutClientTransients(property);
                    latencyHandler.compensate(event, () -> attributes.setMovementSpeed(value));
                } else {
                    double value = property.calcValue();
                    latencyHandler.compensateLazy(event, () -> entities.setMovementSpeed(entityId, value));
                }
            } else if (property.getAttribute() == Attributes.JUMP_STRENGTH
                    || property.getAttribute() == Attributes.HORSE_JUMP_STRENGTH) {
                if (self) {
                    if (!clientObserves(ClientVersion.V_1_20_5)) continue;
                    double value = property.calcValue();
                    latencyHandler.compensate(event, () -> attributes.setJumpStrength(value));
                } else {
                    double value = property.calcValue();
                    latencyHandler.compensateLazy(event, () -> entities.setJumpStrength(entityId, value));
                }
            } else if (property.getAttribute() == Attributes.GRAVITY) {
                if (!clientObserves(ClientVersion.V_1_20_5)) continue;
                double value = property.calcValue();
                if (self) {
                    latencyHandler.compensate(event, () -> attributes.setGravity(value));
                } else {
                    latencyHandler.compensateLazy(event, () -> entities.setGravity(entityId, value));
                }
            } else if (!self && property.getAttribute() == Attributes.FLYING_SPEED) {
                double value = property.calcValue();
                latencyHandler.compensateLazy(event, () -> entities.setFlyingSpeed(entityId, value));
            } else if (property.getAttribute() == Attributes.STEP_HEIGHT) {
                if (!clientObserves(ClientVersion.V_1_20_5)) continue;
                double value = property.calcValue();
                if (self) {
                    latencyHandler.compensate(event, () -> attributes.setStepHeight(value));
                } else {
                    latencyHandler.compensateLazy(event, () -> entities.setStepHeight(entityId, value));
                }
            } else if (self && property.getAttribute() == Attributes.SNEAKING_SPEED) {
                if (!clientObserves(ClientVersion.V_1_21)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setSneakingSpeed(value));
            } else if (self && property.getAttribute() == Attributes.MOVEMENT_EFFICIENCY) {
                if (!clientObserves(ClientVersion.V_1_21)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setMovementEfficiency(value));
            } else if (self && property.getAttribute() == Attributes.WATER_MOVEMENT_EFFICIENCY) {
                if (!clientObserves(ClientVersion.V_1_21)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setWaterMovementEfficiency(value));
            } else if (self && property.getAttribute() == Attributes.FLYING_SPEED) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setFlyingSpeed(value));
            } else if (self && property.getAttribute() == Attributes.SAFE_FALL_DISTANCE) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setSafeFallDistance(value));
            } else if (self && property.getAttribute() == Attributes.FALL_DAMAGE_MULTIPLIER) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setFallDamageMultiplier(value));
            } else if (self && property.getAttribute() == Attributes.AIR_DRAG_MODIFIER) {
                if (!clientObserves(ClientVersion.V_26_2)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setAirDragModifier(value));
            } else if (self && property.getAttribute() == Attributes.FRICTION_MODIFIER) {
                if (!clientObserves(ClientVersion.V_26_2)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setFrictionModifier(value));
            } else if (self && property.getAttribute() == Attributes.BLOCK_BREAK_SPEED) {
                if (!clientObserves(ClientVersion.V_1_20_5)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setBlockBreakSpeed(value));
            } else if (self && property.getAttribute() == Attributes.MINING_EFFICIENCY) {
                if (!clientObserves(ClientVersion.V_1_21)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setMiningEfficiency(value));
            } else if (self && property.getAttribute() == Attributes.SUBMERGED_MINING_SPEED) {
                if (!clientObserves(ClientVersion.V_1_21)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setSubmergedMiningSpeed(value));
            } else if (self && property.getAttribute() == Attributes.BLOCK_INTERACTION_RANGE) {
                if (!clientObserves(ClientVersion.V_1_20_5)) continue;
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setBlockInteractionRange(value));
            }
        }
    }

    private boolean clientObserves(ClientVersion introduced) {
        return player.getClientVersion().isNewerThanOrEquals(introduced);
    }
}
