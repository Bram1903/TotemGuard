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

package com.deathmotion.totemguard.common.replay.capture;

final class PayloadReader {

    private final byte[] payload;
    private int cursor;

    PayloadReader(byte[] payload) {
        this.payload = payload;
    }

    boolean has(int bytes) {
        return cursor + bytes <= payload.length;
    }

    int readVarInt() {
        int value = 0;
        int shift = 0;
        while (has(1)) {
            int b = payload[cursor++] & 0xFF;
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
            if (shift > 35) break;
        }
        throw new IllegalStateException("truncated varint");
    }

    int readUnsignedByte() {
        if (!has(1)) throw new IllegalStateException("truncated byte");
        return payload[cursor++] & 0xFF;
    }

    int readInt() {
        if (!has(4)) throw new IllegalStateException("truncated int");
        return ((payload[cursor++] & 0xFF) << 24)
                | ((payload[cursor++] & 0xFF) << 16)
                | ((payload[cursor++] & 0xFF) << 8)
                | (payload[cursor++] & 0xFF);
    }

    long readLong() {
        if (!has(8)) throw new IllegalStateException("truncated long");
        long value = 0L;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (payload[cursor++] & 0xFFL);
        }
        return value;
    }
}
