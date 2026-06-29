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
import com.deathmotion.totemguard.common.player.data.WorldBorderData;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerInitializeWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorder;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderCenter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWorldBorderSize;

public class OutboundWorldBorderProcessor extends ProcessorOutbound {

    private final WorldBorderData worldBorder;

    public OutboundWorldBorderProcessor(TGPlayer player) {
        super(player);
        this.worldBorder = player.getData().getWorldBorderData();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.INITIALIZE_WORLD_BORDER) {
            WrapperPlayServerInitializeWorldBorder packet = new WrapperPlayServerInitializeWorldBorder(event);
            worldBorder.initialize(packet.getX(), packet.getZ(), packet.getNewDiameter());
        } else if (type == PacketType.Play.Server.WORLD_BORDER_SIZE) {
            worldBorder.setDiameter(new WrapperPlayServerWorldBorderSize(event).getDiameter());
        } else if (type == PacketType.Play.Server.WORLD_BORDER_CENTER) {
            WrapperPlayServerWorldBorderCenter packet = new WrapperPlayServerWorldBorderCenter(event);
            worldBorder.setCenter(packet.getX(), packet.getZ());
        } else if (type == PacketType.Play.Server.WORLD_BORDER) {
            handleLegacy(new WrapperPlayServerWorldBorder(event));
        }
    }

    private void handleLegacy(WrapperPlayServerWorldBorder packet) {
        switch (packet.getAction()) {
            case INITIALIZE -> worldBorder.initialize(packet.getCenterX(), packet.getCenterZ(), packet.getRadius());
            case SET_SIZE -> worldBorder.setDiameter(packet.getRadius());
            case LERP_SIZE -> worldBorder.setDiameter(packet.getNewRadius());
            case SET_CENTER -> worldBorder.setCenter(packet.getCenterX(), packet.getCenterZ());
            default -> {
            }
        }
    }
}
