/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.redis.packet;

import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.util.TGVersions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;

/**
 * Abstract representation of a Packet that can be serialized and deserialized.
 *
 * @param <T> the type of data carried by the packet
 */
@Getter
public abstract class Packet<T> {

    private final int packetId;

    public Packet(int packetId) {
        this.packetId = packetId;
    }

    /**
     * Writes the packet data to a ByteArrayDataOutput stream.
     *
     * @param obj the packet payload
     * @return the data output containing the serialized packet
     */
    public ByteArrayDataOutput write(T obj) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        writeVersion(output);
        output.writeInt(packetId);
        writeData(output, obj);
        return output;
    }

    /**
     * Reads the packet payload from a ByteArrayDataInput stream.
     *
     * @param input the data input containing the serialized packet
     * @return the deserialized packet payload
     */
    public abstract T read(ByteArrayDataInput input);

    /**
     * Writes the packet payload to the output stream.
     *
     * @param output the data output stream
     * @param obj    the packet payload
     */
    public abstract void writeData(ByteArrayDataOutput output, T obj);

    /**
     * Writes the current version to the output stream.
     *
     * @param output the data output stream
     */
    private void writeVersion(ByteArrayDataOutput output) {
        TGVersion version = TGVersions.CURRENT;

        // Write major and minor as bytes (0-255 range assumed)
        output.writeByte(version.major());
        output.writeByte(version.minor());

        // Combine patch and snapshot flag into a single byte
        int patchAndSnapshot = (version.snapshot() ? 0x80 : 0) | (version.patch() & 0x7F);
        output.writeByte(patchAndSnapshot);
    }

}

