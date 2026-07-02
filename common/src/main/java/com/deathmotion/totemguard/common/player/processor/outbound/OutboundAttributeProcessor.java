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
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;

import java.util.List;
import java.util.UUID;

public class OutboundAttributeProcessor extends ProcessorOutbound {

    private static final UUID LEGACY_SPRINT_MODIFIER_ID = UUID.fromString("662a6b8d-da3e-4c1c-8813-96ea6097278d");

    private final PlayerAttributeData attributes;
    private final WorldEntityData worldEntityData;
    private final PacketLatencyHandler latencyHandler;

    public OutboundAttributeProcessor(TGPlayer player) {
        super(player);
        this.attributes = player.getData().getAttributeData();
        this.worldEntityData = player.getData().getWorldEntityData();
        this.latencyHandler = player.getLatencyHandler();
    }

    private static double movementSpeedWithoutSprint(WrapperPlayServerUpdateAttributes.Property property) {
        List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = property.getModifiers();
        if (modifiers.stream().noneMatch(OutboundAttributeProcessor::isSprintModifier)) {
            return property.calcValue();
        }
        List<WrapperPlayServerUpdateAttributes.PropertyModifier> stripped = modifiers.stream()
                .filter(modifier -> !isSprintModifier(modifier))
                .toList();
        return new WrapperPlayServerUpdateAttributes.Property(property.getAttribute(), property.getValue(), stripped)
                .calcValue();
    }

    private static boolean isSprintModifier(WrapperPlayServerUpdateAttributes.PropertyModifier modifier) {
        ResourceLocation name = modifier.getName();
        if (name != null && "minecraft".equals(name.getNamespace()) && "sprinting".equals(name.getKey())) {
            return true;
        }
        return LEGACY_SPRINT_MODIFIER_ID.equals(modifier.getUUID());
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
                double value = property.calcValue();
                if (self) {
                    latencyHandler.compensate(event, () -> attributes.setScale(value));
                } else {
                    worldEntityData.setScale(entityId, value);
                }
            } else if (self && property.getAttribute() == Attributes.MOVEMENT_SPEED) {
                double value = movementSpeedWithoutSprint(property);
                latencyHandler.compensate(event, () -> attributes.setMovementSpeed(value));
            } else if (self && property.getAttribute() == Attributes.JUMP_STRENGTH) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setJumpStrength(value));
            } else if (self && property.getAttribute() == Attributes.GRAVITY) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setGravity(value));
            } else if (self && property.getAttribute() == Attributes.STEP_HEIGHT) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setStepHeight(value));
            } else if (self && property.getAttribute() == Attributes.SNEAKING_SPEED) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setSneakingSpeed(value));
            } else if (self && property.getAttribute() == Attributes.MOVEMENT_EFFICIENCY) {
                double value = property.calcValue();
                latencyHandler.compensate(event, () -> attributes.setMovementEfficiency(value));
            } else if (self && property.getAttribute() == Attributes.WATER_MOVEMENT_EFFICIENCY) {
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
            }
        }
    }
}
