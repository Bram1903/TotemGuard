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

import com.deathmotion.totemguard.common.check.CheckManagerImpl;
import com.deathmotion.totemguard.common.player.PlayerRepositoryImpl;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

public class PacketCheckManagerListener extends PacketListenerAbstract {

    private final PlayerRepositoryImpl playerRepository;

    public PacketCheckManagerListener(PlayerRepositoryImpl playerRepository) {
        super(PacketListenerPriority.LOW);
        this.playerRepository = playerRepository;
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        TGPlayer player = playerRepository.getPlayer(event.getUser());
        if (player == null) return;

        final ConnectionState state = event.getConnectionState();
        final ProcessorInbound[] inbounds = player.getProcessorInbounds();

        if (state != ConnectionState.PLAY) {
            if (state != ConnectionState.CONFIGURATION) return;
            for (ProcessorInbound processor : inbounds) {
                processor.handleInbound(event);
            }
            return;
        }

        final CheckManagerImpl checkManager = player.getCheckManager();
        final PacketTypeCommon packetType = event.getPacketType();
        final boolean flying = WrapperPlayClientPlayerFlying.isFlying(packetType);

        if (flying) {
            checkManager.onPreFlying(event);
        }

        for (ProcessorInbound processor : inbounds) {
            processor.handleInbound(event);
        }

        if (flying) {
            if (!event.isCancelled()) {
                player.getPhysics().onFlying();
                player.getData().updateNetherPortalContact();
                player.getDebugOverlayManager().refresh();
            }
        } else if (packetType == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTick()) {
            player.getPhysics().onTickEnd();
        }
        checkManager.onPacketReceive(event);
        player.triggerInventoryEvent();

        for (ProcessorInbound processor : inbounds) {
            processor.handleInboundPost(event);
        }
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        TGPlayer player = playerRepository.getPlayer(event.getUser());
        if (player == null) return;

        final ProcessorOutbound[] outbounds = player.getProcessorOutbounds();

        for (ProcessorOutbound processor : outbounds) {
            processor.handleOutbound(event);
        }

        player.getCheckManager().onPacketSend(event);
        player.triggerInventoryEvent();

        for (ProcessorOutbound processor : outbounds) {
            processor.handleOutboundPost(event);
        }
    }
}
