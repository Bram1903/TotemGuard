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

package com.deathmotion.totemguard.common.event.packet;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;

public class PacketCheckManagerListener extends PacketListenerAbstract {

    public PacketCheckManagerListener() {
        super(PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        TGPlayer player = TGPlatform.getInstance().getPlayerRepository().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getConnectionState() != ConnectionState.PLAY) {
            if (event.getConnectionState() != ConnectionState.CONFIGURATION) return;
            for (ProcessorInbound processor : player.getProcessorInbounds()) {
                processor.handleInbound(event);
            }
            return;
        }

        for (ProcessorInbound processor : player.getProcessorInbounds()) {
            processor.handleInbound(event);
        }

        player.getCheckManager().onPacketReceive(event);
        player.triggerInventoryEvent();

        for (ProcessorInbound processor : player.getProcessorInbounds()) {
            processor.handleInboundPost(event);
        }
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        TGPlayer player = TGPlatform.getInstance().getPlayerRepository().getPlayer(event.getUser());
        if (player == null) return;

        for (ProcessorOutbound processor : player.getProcessorOutbounds()) {
            processor.handleOutbound(event);
        }

        player.getCheckManager().onPacketSend(event);
        player.triggerInventoryEvent();

        for (ProcessorOutbound processor : player.getProcessorOutbounds()) {
            processor.handleOutboundPost(event);
        }
    }
}
