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
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.protocol.world.states.type.StateType;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;

public class OutboundPistonProcessor extends ProcessorOutbound {

    private static final double PUSH_RANGE_SQUARED = 3.0 * 3.0;
    private static final int PUSH_WINDOW_TICKS = 6;

    private final Data data;
    private final PacketLatencyHandler latencyHandler;

    public OutboundPistonProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.BLOCK_ACTION) return;

        WrapperPlayServerBlockAction packet = new WrapperPlayServerBlockAction(event);
        StateType type = packet.getBlockType().getType();
        if (type != StateTypes.PISTON && type != StateTypes.STICKY_PISTON) return;

        Vector3i pos = packet.getBlockPosition();
        latencyHandler.compensate(event, () -> armIfNear(pos));
    }

    private void armIfNear(Vector3i pos) {
        Location current = data.getMovementData().getCurrent();
        double dx = (pos.getX() + 0.5) - current.getX();
        double dy = (pos.getY() + 0.5) - current.getY();
        double dz = (pos.getZ() + 0.5) - current.getZ();
        if (dx * dx + dy * dy + dz * dz <= PUSH_RANGE_SQUARED) {
            data.getPistonData().arm(PUSH_WINDOW_TICKS);
        }
    }
}
