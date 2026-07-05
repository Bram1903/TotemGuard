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
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.deathmotion.totemguard.common.world.border.BorderMirror;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerInitializeWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderCenter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayWorldBorderLerpSize;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderSize;

public class OutboundWorldBorderProcessor extends ProcessorOutbound {

    private final BorderMirror border;
    private final PacketLatencyHandler latencyHandler;

    public OutboundWorldBorderProcessor(TGPlayer player) {
        super(player);
        this.border = player.getWorldMirror().border();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.INITIALIZE_WORLD_BORDER) {
            WrapperPlayServerInitializeWorldBorder packet = new WrapperPlayServerInitializeWorldBorder(event);
            final double x = packet.getX();
            final double z = packet.getZ();
            final double oldDiameter = packet.getOldDiameter();
            final double newDiameter = packet.getNewDiameter();
            final long speed = packet.getSpeed();
            latencyHandler.compensateLazy(event, () -> {
                border.initialize(x, z, oldDiameter);
                if (speed > 0 && oldDiameter != newDiameter) {
                    border.lerpDiameter(oldDiameter, newDiameter, speed);
                } else {
                    border.setDiameter(newDiameter);
                }
            });
        } else if (type == PacketType.Play.Server.WORLD_BORDER_SIZE) {
            final double diameter = new WrapperPlayServerWorldBorderSize(event).getDiameter();
            latencyHandler.compensateLazy(event, () -> border.setDiameter(diameter));
        } else if (type == PacketType.Play.Server.WORLD_BORDER_LERP_SIZE) {
            WrapperPlayWorldBorderLerpSize packet = new WrapperPlayWorldBorderLerpSize(event);
            final double oldDiameter = packet.getOldDiameter();
            final double newDiameter = packet.getNewDiameter();
            final long speed = packet.getSpeed();
            latencyHandler.compensateLazy(event, () -> border.lerpDiameter(oldDiameter, newDiameter, speed));
        } else if (type == PacketType.Play.Server.WORLD_BORDER_CENTER) {
            WrapperPlayServerWorldBorderCenter packet = new WrapperPlayServerWorldBorderCenter(event);
            final double x = packet.getX();
            final double z = packet.getZ();
            latencyHandler.compensateLazy(event, () -> border.setCenter(x, z));
        } else if (type == PacketType.Play.Server.WORLD_BORDER) {
            handleLegacy(event, new WrapperPlayServerWorldBorder(event));
        }
    }

    private void handleLegacy(PacketSendEvent event, WrapperPlayServerWorldBorder packet) {
        switch (packet.getAction()) {
            case INITIALIZE -> {
                final double x = packet.getCenterX();
                final double z = packet.getCenterZ();
                final double radius = packet.getRadius();
                latencyHandler.compensateLazy(event, () -> border.initialize(x, z, radius));
            }
            case SET_SIZE -> {
                final double radius = packet.getRadius();
                latencyHandler.compensateLazy(event, () -> border.setDiameter(radius));
            }
            case LERP_SIZE -> {
                final double oldRadius = packet.getOldRadius();
                final double newRadius = packet.getNewRadius();
                final long speed = packet.getSpeed();
                latencyHandler.compensateLazy(event, () -> border.lerpDiameter(oldRadius, newRadius, speed));
            }
            case SET_CENTER -> {
                final double x = packet.getCenterX();
                final double z = packet.getCenterZ();
                latencyHandler.compensateLazy(event, () -> border.setCenter(x, z));
            }
            default -> {
            }
        }
    }
}
