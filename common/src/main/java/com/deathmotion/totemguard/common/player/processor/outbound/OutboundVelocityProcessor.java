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
import com.deathmotion.totemguard.common.player.data.ExternalVelocityData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;

public class OutboundVelocityProcessor extends ProcessorOutbound {

    private final ExternalVelocityData externalVelocity;
    private final PacketLatencyHandler latencyHandler;

    public OutboundVelocityProcessor(TGPlayer player) {
        super(player);
        this.externalVelocity = player.getData().getExternalVelocityData();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.ENTITY_VELOCITY) {
            WrapperPlayServerEntityVelocity packet = new WrapperPlayServerEntityVelocity(event);
            int entityId = packet.getEntityId();
            if (entityId == player.getUser().getEntityId()) {
                Vector3d velocity = packet.getVelocity();
                if (velocity == null) return;
                final double vx = velocity.getX();
                final double vy = velocity.getY();
                final double vz = velocity.getZ();
                latencyHandler.compensate(event, () -> externalVelocity.setVelocity(vx, vy, vz));
                return;
            }
            if (entityId == player.getData().getVehicleId()) {
                Vector3d velocity = packet.getVelocity();
                if (velocity == null) return;
                final double vx = velocity.getX();
                final double vy = velocity.getY();
                final double vz = velocity.getZ();
                latencyHandler.compensate(event, () -> {
                    if (entityId == player.getData().getVehicleId()) {
                        player.getData().getVehicleData().addImpulse(vx, vy, vz);
                    }
                });
            }
        } else if (type == PacketType.Play.Server.EXPLOSION) {
            WrapperPlayServerExplosion packet = new WrapperPlayServerExplosion(event);
            pushOnClientAck(event, packet.getKnockback());
        }
    }

    private void pushOnClientAck(PacketSendEvent event, Vector3d velocity) {
        if (velocity == null) return;
        final double vx = velocity.getX();
        final double vy = velocity.getY();
        final double vz = velocity.getZ();
        latencyHandler.compensate(event, () -> externalVelocity.addPush(vx, vy, vz));
    }
}
