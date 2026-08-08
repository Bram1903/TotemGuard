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

public final class SyncCheckResultPacket extends Packet<SyncCheckResultPacket.Payload> {

    public static final byte STATUS_NOT_FOUND = 0;
    public static final byte STATUS_ALREADY_CHECKING = 1;
    public static final byte STATUS_ON_COOLDOWN = 2;
    public static final byte STATUS_WRONG_GAMEMODE = 3;
    public static final byte STATUS_INVULNERABLE = 4;
    public static final byte STATUS_NO_TOTEM = 5;
    public static final byte STATUS_DAMAGE_FAILED = 6;
    public static final byte STATUS_FLAGGED = 7;
    public static final byte STATUS_PASSED = 8;
    public static final byte STATUS_NO_BACKUP_TOTEM = 9;

    public SyncCheckResultPacket(int id) {
        super(id, MessagingTopic.PRESENCE);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID requestId = PacketIO.readUUID(input);
        UUID senderInstanceId = PacketIO.readUUID(input);
        UUID senderUuid = PacketIO.readUUID(input);
        UUID targetUuid = PacketIO.readUUID(input);
        String targetName = input.readUTF();
        byte status = input.readByte();
        long elapsedMs = input.readLong();
        long remainingMs = input.readLong();
        int durationMs = input.readInt();
        return new Payload(requestId, senderInstanceId, senderUuid, targetUuid, targetName,
                status, elapsedMs, remainingMs, durationMs);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.requestId);
        PacketIO.writeUUID(output, payload.senderInstanceId);
        PacketIO.writeUUID(output, payload.senderUuid);
        PacketIO.writeUUID(output, payload.targetUuid);
        output.writeUTF(payload.targetName);
        output.writeByte(payload.status);
        output.writeLong(payload.elapsedMs);
        output.writeLong(payload.remainingMs);
        output.writeInt(payload.durationMs);
    }

    public record Payload(
            UUID requestId,
            UUID senderInstanceId,
            UUID senderUuid,
            UUID targetUuid,
            String targetName,
            byte status,
            long elapsedMs,
            long remainingMs,
            int durationMs
    ) {
    }
}
