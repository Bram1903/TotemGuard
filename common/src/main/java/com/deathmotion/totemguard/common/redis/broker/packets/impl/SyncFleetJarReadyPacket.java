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

/**
 * Broadcast by the instance that downloaded a jar for a given resolution key, telling
 * other instances on the same resolution key that the bytes are now available in
 * Redis under {@code totemguard:fleet:bytes:<resolutionKey>}.
 */
public final class SyncFleetJarReadyPacket extends Packet<SyncFleetJarReadyPacket.Payload> {

    public SyncFleetJarReadyPacket(int id) {
        super(id, MessagingTopic.UPDATES);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID requestId = PacketIO.readUUID(input);
        String resolutionKey = input.readUTF();
        String source = input.readUTF();
        String version = input.readUTF();
        String sha256 = input.readUTF();
        String fileName = input.readUTF();
        long sizeBytes = input.readLong();
        return new Payload(requestId, resolutionKey, source, version, sha256, fileName, sizeBytes);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.requestId);
        output.writeUTF(payload.resolutionKey);
        output.writeUTF(payload.source);
        output.writeUTF(payload.version);
        output.writeUTF(payload.sha256);
        output.writeUTF(payload.fileName);
        output.writeLong(payload.sizeBytes);
    }

    public record Payload(
            UUID requestId,
            String resolutionKey,
            String source,
            String version,
            String sha256,
            String fileName,
            long sizeBytes
    ) {
    }
}
