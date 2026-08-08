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

package com.deathmotion.totemguard.common.redis.broker.packets;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

// Frame: u8 version | utf senderId | i32 packetId | <Packet#writeData payload>
public final class PacketCodec {

    public static final int PROTOCOL_VERSION = 1;

    private PacketCodec() {
    }

    public static <T> byte[] encode(String senderId, Packet<T> packet, T payload) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeByte(PROTOCOL_VERSION);
        output.writeUTF(senderId);
        output.writeInt(packet.getId());
        packet.writeData(output, payload);
        return output.toByteArray();
    }

    public static Frame decode(byte[] message) {
        ByteArrayDataInput input = ByteStreams.newDataInput(message);
        int protocolVersion = input.readUnsignedByte();
        String senderId = input.readUTF();
        int packetId = input.readInt();
        return new Frame(protocolVersion, senderId, packetId, input);
    }

    public record Frame(int protocolVersion, String senderId, int packetId, ByteArrayDataInput payload) {
    }
}
