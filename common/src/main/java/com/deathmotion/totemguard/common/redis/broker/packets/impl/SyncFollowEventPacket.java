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

public final class SyncFollowEventPacket extends Packet<SyncFollowEventPacket.Payload> {

    public static final byte KIND_START = 1;
    public static final byte KIND_END = 2;
    public static final byte KIND_MOVE = 3;

    public SyncFollowEventPacket(int id) {
        super(id, MessagingTopic.PRESENCE);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        byte kind = input.readByte();
        UUID followerUuid = PacketIO.readUUID(input);
        UUID targetUuid = PacketIO.readUUID(input);
        UUID followerServerInstance = PacketIO.readUUID(input);
        UUID targetServerInstance = PacketIO.readUUID(input);
        String targetServerName = input.readUTF();
        String world = input.readUTF();
        double x = input.readDouble();
        double y = input.readDouble();
        double z = input.readDouble();
        float yaw = input.readFloat();
        float pitch = input.readFloat();
        return new Payload(kind, followerUuid, targetUuid,
                followerServerInstance, targetServerInstance, targetServerName,
                world, x, y, z, yaw, pitch);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        output.writeByte(payload.kind);
        PacketIO.writeUUID(output, payload.followerUuid);
        PacketIO.writeUUID(output, payload.targetUuid);
        PacketIO.writeUUID(output, payload.followerServerInstance);
        PacketIO.writeUUID(output, payload.targetServerInstance);
        output.writeUTF(payload.targetServerName);
        output.writeUTF(payload.world);
        output.writeDouble(payload.x);
        output.writeDouble(payload.y);
        output.writeDouble(payload.z);
        output.writeFloat(payload.yaw);
        output.writeFloat(payload.pitch);
    }

    public record Payload(
            byte kind,
            UUID followerUuid,
            UUID targetUuid,
            UUID followerServerInstance,
            UUID targetServerInstance,
            String targetServerName,
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
    }
}
