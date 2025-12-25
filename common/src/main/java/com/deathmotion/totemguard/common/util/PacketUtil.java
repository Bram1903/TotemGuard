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

package com.deathmotion.totemguard.common.util;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class PacketUtil {

    public final int TOTEM_USE_STATUS = 35;

    public boolean isTotemPacket(PacketSendEvent event, int playerEntityId) {
        if (event.isCancelled()) return false;

        if (event.getPacketType() != PacketType.Play.Server.ENTITY_STATUS) return false;
        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(event);
        if (packet.getEntityId() != playerEntityId) return false;
        return packet.getStatus() == TOTEM_USE_STATUS;
    }
}
