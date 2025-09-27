/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.events.packets;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientWindowConfirmation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;

public class PacketPingListener extends PacketListenerAbstract {

    /**
     * Bitmask to convert a signed Java short (-32,768–32,767)
     * into an unsigned 16-bit integer (0–65,535).
     * This is required because Minecraft uses unsigned shorts
     * for transaction/keepalive IDs, while Java does not.
     */
    private static final int UNSIGNED_SHORT_MASK = 0xFFFF;

    public PacketPingListener() {
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        TotemPlayer player = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Client.WINDOW_CONFIRMATION) {
            WrapperPlayClientWindowConfirmation wrapper = new WrapperPlayClientWindowConfirmation(event);
            int id = wrapper.getActionId() & UNSIGNED_SHORT_MASK;
            player.pingData.addTransactionResponse(id);
        }

        if (event.getPacketType() == PacketType.Play.Client.PONG) {
            WrapperPlayClientPong wrapper = new WrapperPlayClientPong(event);
            player.pingData.addTransactionResponse(wrapper.getId());
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        TotemPlayer player = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
            WrapperPlayServerWindowConfirmation wrapper = new WrapperPlayServerWindowConfirmation(event);
            int id = wrapper.getActionId() & UNSIGNED_SHORT_MASK;
            player.pingData.addTransactionSent(id);
        }

        if (event.getPacketType() == PacketType.Play.Server.PING) {
            WrapperPlayServerPing wrapper = new WrapperPlayServerPing(event);
            player.pingData.addTransactionSent(wrapper.getId());
        }
    }
}
