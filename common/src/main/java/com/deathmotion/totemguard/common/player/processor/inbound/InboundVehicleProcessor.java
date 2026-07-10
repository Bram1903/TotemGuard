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
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;

public class InboundVehicleProcessor extends ProcessorInbound {

    public InboundVehicleProcessor(TGPlayer player) {
        super(player);
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Client.VEHICLE_MOVE) return;

        WrapperPlayClientVehicleMove packet = new WrapperPlayClientVehicleMove(event);
        Vector3d pos = packet.getPosition();
        boolean cancel = player.getVehicleEngine().onVehicleMove(
                pos.getX(), pos.getY(), pos.getZ(), packet.getYaw(), packet.getPitch());
        if (cancel) event.setCancelled(true);
    }
}
