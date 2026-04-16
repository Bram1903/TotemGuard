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

package com.deathmotion.totemguard.common.player.processor.inbound;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.data.PingData;
import com.deathmotion.totemguard.common.player.data.TeleportData;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientTeleportConfirm;

public class InboundTeleportProcessor extends ProcessorInbound {

    private final Data data;
    private final PingData pingData;

    public InboundTeleportProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.pingData = player.getPingData();
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.TELEPORT_CONFIRM) return;
        WrapperPlayClientTeleportConfirm packet = new WrapperPlayClientTeleportConfirm(event);
        TeleportData.TeleportConfirmResult confirmResult = data.getTeleportData().validateTeleportConfirm(packet.getTeleportId());
        data.getMovementData().handleTeleportConfirm(confirmResult);
        pingData.teleportReceived(packet.getTeleportId(), event.getTimestamp());
        player.getDebugOverlayManager().refresh();
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            data.getTeleportData().clearLastPacketWasTeleport();
        }
    }
}
