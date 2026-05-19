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

public final class SyncCheckRequestPacket extends Packet<SyncCheckRequestPacket.Payload> {

    public SyncCheckRequestPacket(int id) {
        super(id, MessagingTopic.PRESENCE);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID requestId = PacketIO.readUUID(input);
        UUID senderInstanceId = PacketIO.readUUID(input);
        UUID senderUuid = PacketIO.readUUID(input);
        String senderName = input.readUTF();
        String senderServerName = input.readUTF();
        UUID targetServerInstanceId = PacketIO.readUUID(input);
        UUID targetUuid = PacketIO.readUUID(input);
        int durationMs = input.readInt();
        long expiresAt = input.readLong();
        return new Payload(requestId, senderInstanceId, senderUuid, senderName, senderServerName,
                targetServerInstanceId, targetUuid, durationMs, expiresAt);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.requestId);
        PacketIO.writeUUID(output, payload.senderInstanceId);
        PacketIO.writeUUID(output, payload.senderUuid);
        output.writeUTF(payload.senderName);
        output.writeUTF(payload.senderServerName);
        PacketIO.writeUUID(output, payload.targetServerInstanceId);
        PacketIO.writeUUID(output, payload.targetUuid);
        output.writeInt(payload.durationMs);
        output.writeLong(payload.expiresAt);
    }

    public record Payload(
            UUID requestId,
            UUID senderInstanceId,
            UUID senderUuid,
            String senderName,
            String senderServerName,
            UUID targetServerInstanceId,
            UUID targetUuid,
            int durationMs,
            long expiresAt
    ) {
    }
}
