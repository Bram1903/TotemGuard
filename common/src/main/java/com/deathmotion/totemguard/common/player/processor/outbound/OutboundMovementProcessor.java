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
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerRotation;

public class OutboundMovementProcessor extends ProcessorOutbound {

    private final MovementData movementData;

    public OutboundMovementProcessor(TGPlayer player) {
        super(player);
        this.movementData = player.getData().getMovementData();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            movementData.handleServerSync(new WrapperPlayServerPlayerPositionAndLook(event));
        } else if (type == PacketType.Play.Server.PLAYER_ROTATION) {
            movementData.handleServerSync(new WrapperPlayServerPlayerRotation(event));
        }
    }
}
