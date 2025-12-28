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

package com.deathmotion.totemguard.common.player.processor.inbound;

import com.deathmotion.totemguard.common.player.PacketStateData;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;

public class InboundActionProcessor extends ProcessorInbound {

    public InboundActionProcessor(TGPlayer player) {
        super(player);
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            player.getPacketStateData().setPlacedBlockThisTick(true);
        } else if (packetType == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

            switch (packet.getAction()) {
                case START_SNEAKING -> player.getPacketStateData().setSneaking(true);
                case STOP_SNEAKING -> player.getPacketStateData().setSneaking(false);
                case START_SPRINTING -> player.getPacketStateData().setSprinting(true);
                case STOP_SPRINTING -> player.getPacketStateData().setSprinting(false);
            }
        } else if (packetType == PacketType.Play.Client.PLAYER_INPUT) {
            if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_6)) {
                WrapperPlayClientPlayerInput input = new WrapperPlayClientPlayerInput(event);
                PacketStateData data = player.getPacketStateData();
                data.setSneaking(input.isShift());
            }
        } else if (packetType == PacketType.Play.Client.PLAYER_ABILITIES) {
            WrapperPlayClientPlayerAbilities packet = new WrapperPlayClientPlayerAbilities(event);
            PacketStateData data = player.getPacketStateData();
            data.setFlying(packet.isFlying() && data.isCanFly());
        }
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        if (!player.isTickEndPacket(event.getPacketType())) return;
        player.getPacketStateData().setPlacedBlockThisTick(false);
    }
}
