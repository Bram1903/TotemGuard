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

package com.deathmotion.totemguard.packets;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.Getter;

@Getter
public abstract class Packet<T> {

    private final int packetId;

    public Packet(int packetId) {
        this.packetId = packetId;
    }

    public ByteArrayDataOutput invoke(T obj) {
        return write(obj);
    }

    private ByteArrayDataOutput write(T obj) {
        ByteArrayDataOutput bytes = ByteStreams.newDataOutput();
        writeId(bytes);
        write(bytes, obj);
        return bytes;
    }

    public abstract T read(ByteArrayDataInput bytes);

    public abstract void write(ByteArrayDataOutput bytes, T obj);

    private void writeId(ByteArrayDataOutput bytes) {
        bytes.writeInt(packetId);
    }
}
