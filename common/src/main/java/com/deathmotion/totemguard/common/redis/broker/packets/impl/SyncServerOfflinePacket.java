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

public final class SyncServerOfflinePacket extends Packet<SyncServerOfflinePacket.Payload> {

    public SyncServerOfflinePacket(int id) {
        super(id, MessagingTopic.PRESENCE);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID instanceId = PacketIO.readUUID(input);
        String serverName = input.readUTF();
        return new Payload(instanceId, serverName);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.instanceId);
        output.writeUTF(payload.serverName);
    }

    public record Payload(UUID instanceId, String serverName) {
    }
}
