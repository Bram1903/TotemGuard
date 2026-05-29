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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SyncNetworkAlertPacket extends Packet<SyncNetworkAlertPacket.Payload> {

    public SyncNetworkAlertPacket(int id) {
        super(id, MessagingTopic.EVENTS);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        UUID playerUuid = PacketIO.readUUID(input);
        String playerName = input.readUTF();
        String checkName = input.readUTF();
        String checkType = input.readUTF();
        int violations = input.readInt();
        String debug = PacketIO.readOptionalString(input);
        String serverName = input.readUTF();
        boolean flag = input.readBoolean();
        long timestamp = input.readLong();
        return new Payload(playerUuid, playerName, checkName, checkType, violations, debug, serverName, flag, timestamp);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        PacketIO.writeUUID(output, payload.playerUuid());
        output.writeUTF(payload.playerName());
        output.writeUTF(payload.checkName());
        output.writeUTF(payload.checkType());
        output.writeInt(payload.violations());
        PacketIO.writeOptionalString(output, payload.debug());
        output.writeUTF(payload.serverName());
        output.writeBoolean(payload.flag());
        output.writeLong(payload.timestamp());
    }

    public record Payload(UUID playerUuid, String playerName, String checkName, String checkType,
                          int violations, @Nullable String debug, String serverName, boolean flag, long timestamp) {
    }
}
