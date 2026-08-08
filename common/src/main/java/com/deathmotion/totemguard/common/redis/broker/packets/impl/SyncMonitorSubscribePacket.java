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

package com.deathmotion.totemguard.common.redis.broker.packets.impl;

import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketIO;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import java.util.UUID;

public final class SyncMonitorSubscribePacket extends Packet<SyncMonitorSubscribePacket.Payload> {

    public SyncMonitorSubscribePacket(int id) {
        super(id, MessagingTopic.PRESENCE);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID viewerInstanceId = PacketIO.readUUID(input);
        UUID targetUuid = PacketIO.readUUID(input);
        return new Payload(viewerInstanceId, targetUuid);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.viewerInstanceId);
        PacketIO.writeUUID(output, payload.targetUuid);
    }

    public record Payload(UUID viewerInstanceId, UUID targetUuid) {
    }
}
