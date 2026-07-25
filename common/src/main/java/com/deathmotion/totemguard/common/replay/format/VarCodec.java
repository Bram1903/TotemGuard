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

package com.deathmotion.totemguard.common.replay.format;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class VarCodec {

    private VarCodec() {
    }

    public static void writeUnsigned(DataOutputStream out, long value) throws IOException {
        long remaining = value;
        while ((remaining & ~0x7FL) != 0L) {
            out.writeByte((int) (remaining & 0x7FL) | 0x80);
            remaining >>>= 7;
        }
        out.writeByte((int) remaining);
    }

    public static long readUnsigned(DataInputStream in) throws IOException {
        long value = 0L;
        int shift = 0;
        while (true) {
            int b = in.readUnsignedByte();
            value |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
            if (shift > 63) throw new IOException("VarInt too long");
        }
    }

    public static void writeSigned(DataOutputStream out, long value) throws IOException {
        writeUnsigned(out, (value << 1) ^ (value >> 63));
    }

    public static long readSigned(DataInputStream in) throws IOException {
        long raw = readUnsigned(in);
        return (raw >>> 1) ^ -(raw & 1L);
    }

    public static void writeInt(DataOutputStream out, int value) throws IOException {
        writeUnsigned(out, Integer.toUnsignedLong(value));
    }

    public static int readInt(DataInputStream in) throws IOException {
        return (int) readUnsigned(in);
    }
}
