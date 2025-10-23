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
import com.deathmotion.totemguard.util.PacketTypeSets;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

public class CheckManagerPacketListener extends PacketListenerAbstract {

    public CheckManagerPacketListener() {
        super(PacketListenerPriority.LOW);
    }

    private static boolean isPlayerTriggeredPacket(final PacketTypeCommon packetType) {
        return PacketTypeSets.PLAYER_TRIGGERED.contains(packetType);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        TotemPlayer player = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getConnectionState() != ConnectionState.PLAY) {
            // Allow checks to listen to configuration packets
            if (event.getConnectionState() != ConnectionState.CONFIGURATION) return;
            player.checkManager.onPacketReceive(event);
            return;
        }

        if (isPlayerTriggeredPacket(event.getPacketType())) {
            player.runExtraDelayedChecks();
        }

        player.checkManager.onPacketReceive(event);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) return;
        TotemPlayer player = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getUser());
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Server.BUNDLE) {
            player.sendingBundlePacket = !player.sendingBundlePacket;
        }

        player.checkManager.onPacketSend(event);
    }
}
