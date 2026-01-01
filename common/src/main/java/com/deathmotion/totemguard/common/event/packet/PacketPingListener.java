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
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;

public final class PacketPingListener extends PacketListenerAbstract {

    public PacketPingListener() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final TGPlayer player = TGPlatform.getInstance().getPlayerRepository().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            final WrapperPlayClientWindowConfirmation transaction = new WrapperPlayClientWindowConfirmation(event);
            final short id = transaction.getActionId();

            if (id <= 0) {
                final boolean handledByTotemGuard = player.getLatencyHandler().onTransactionResponse(id);
                if (handledByTotemGuard) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        // 1.17+: PONG is the response to our PING
        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            final WrapperPlayClientPong pong = new WrapperPlayClientPong(event);
            final int id = pong.getId();

            if (id == (short) id) {
                final short shortId = (short) id;
                player.getLatencyHandler().onTransactionResponse(shortId);
            }
        }
    }
}
