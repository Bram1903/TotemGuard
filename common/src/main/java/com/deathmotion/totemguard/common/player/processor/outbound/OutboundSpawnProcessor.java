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
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;

public class OutboundSpawnProcessor extends ProcessorOutbound {

    private final Data data;

    public OutboundSpawnProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.JOIN_GAME) {
            // On join, we don't really care about lag compensation
            WrapperPlayServerJoinGame packet = new WrapperPlayServerJoinGame(event);
            data.setGameMode(packet.getGameMode());
        } else if (packetType == PacketType.Play.Server.RESPAWN) {
            WrapperPlayServerRespawn packet = new WrapperPlayServerRespawn(event);

            // Make sure to delay this since it's a respawn packet
            player.getLatencyHandler().afterNextAckDelayed(event, () -> {
                // Thanks Grim
                if (player.getClientVersion().isOlderThan(ClientVersion.V_1_16) || player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20)) {
                    data.setSneaking(false);
                }

                data.setSprinting(false);
                data.setGameMode(packet.getGameMode());
                data.setOpenInventory(false);
            });
        }
    }
}