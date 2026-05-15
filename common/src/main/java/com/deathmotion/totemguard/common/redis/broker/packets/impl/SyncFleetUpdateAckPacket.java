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
 * Status report sent from each fleet member back to the originating instance after
 * processing a fleet update request. Routed via per-instance unicast.
 */
public final class SyncFleetUpdateAckPacket extends Packet<SyncFleetUpdateAckPacket.Payload> {

    public SyncFleetUpdateAckPacket(int id) {
        super(id, MessagingTopic.PRESENCE);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID requestId = PacketIO.readUUID(input);
        UUID instanceId = PacketIO.readUUID(input);
        String serverName = input.readUTF();
        String status = input.readUTF();
        String version = input.readUTF();
        String source = input.readUTF();
        String detail = input.readUTF();
        return new Payload(requestId, instanceId, serverName, status, version, source, detail);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.requestId);
        PacketIO.writeUUID(output, payload.instanceId);
        output.writeUTF(payload.serverName);
        output.writeUTF(payload.status);
        output.writeUTF(payload.version);
        output.writeUTF(payload.source);
        output.writeUTF(payload.detail);
    }

    public record Payload(
            UUID requestId,
            UUID instanceId,
            String serverName,
            String status,
            String version,
            String source,
            String detail
    ) {

        public static final String STATUS_STAGED = "STAGED";
        public static final String STATUS_RESTARTING = "RESTARTING";
        public static final String STATUS_DRY_RUN = "DRY_RUN";
        public static final String STATUS_SKIPPED = "SKIPPED";
        public static final String STATUS_FAILED = "FAILED";
    }
}
