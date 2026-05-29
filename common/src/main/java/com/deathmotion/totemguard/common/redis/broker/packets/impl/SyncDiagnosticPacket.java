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

public class SyncDiagnosticPacket extends Packet<SyncDiagnosticPacket.Payload> {

    public SyncDiagnosticPacket(int id) {
        super(id, MessagingTopic.EVENTS);
    }

    @Override
    public Payload read(ByteArrayDataInput input) {
        String severity = input.readUTF();
        String subsystem = input.readUTF();
        String message = input.readUTF();
        String stackTrace = PacketIO.readOptionalString(input);
        String serverName = input.readUTF();
        long timestamp = input.readLong();
        return new Payload(severity, subsystem, message, stackTrace, serverName, timestamp);
    }

    @Override
    public void writeData(ByteArrayDataOutput output, Payload payload) {
        output.writeUTF(payload.severity());
        output.writeUTF(payload.subsystem());
        output.writeUTF(payload.message());
        PacketIO.writeOptionalString(output, payload.stackTrace());
        output.writeUTF(payload.serverName());
        output.writeLong(payload.timestamp());
    }

    public record Payload(String severity, String subsystem, String message,
                          @Nullable String stackTrace, String serverName, long timestamp) {
    }
}
