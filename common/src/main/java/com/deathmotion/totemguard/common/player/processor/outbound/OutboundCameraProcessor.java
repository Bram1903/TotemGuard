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
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.player.data.WorldEntityData;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;

public class OutboundCameraProcessor extends ProcessorOutbound {

    private final MovementData movementData;
    private final WorldEntityData worldEntityData;
    private final PacketLatencyHandler latencyHandler;

    public OutboundCameraProcessor(TGPlayer player) {
        super(player);
        this.movementData = player.getData().getMovementData();
        this.worldEntityData = player.getData().getWorldEntityData();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.CAMERA) return;

        int cameraId = new WrapperPlayServerCamera(event).getCameraId();
        boolean isSelf = cameraId == player.getUser().getEntityId();

        // Per protocol: "If the given entity is not loaded by the player, this packet is ignored."
        // The tracker reflects the client's loaded-entity set at this packet's send time, so a
        // miss here means the client will silently drop the CAMERA and we must not flip state.
        if (!isSelf && !worldEntityData.isLoaded(cameraId)) return;

        latencyHandler.compensate(event, () -> movementData.handleCameraChange(isSelf));
    }
}
