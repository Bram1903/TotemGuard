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

package com.deathmotion.totemguard.common.replay.viewer.net;

import java.util.HashMap;
import java.util.Map;

public final class ViewerEntityIds {

    private final Map<Integer, Integer> remap = new HashMap<>(4);

    public void map(int from, int to) {
        if (from == to) return;
        remap.put(from, to);
    }

    public int map(int id) {
        Integer mapped = remap.get(id);
        return mapped == null ? id : mapped;
    }

    public byte[] rewrite(Shape shape, byte[] payload) {
        if (remap.isEmpty()) return payload;
        return switch (shape) {
            case LEADING_VARINT -> rewriteLeadingVarInt(payload);
            case LEADING_INT -> rewriteLeadingInt(payload);
            case VARINT_LIST -> rewriteVarIntList(payload, false);
            case VEHICLE_AND_LIST -> rewriteVarIntList(payload, true);
        };
    }

    private byte[] rewriteLeadingVarInt(byte[] payload) {
        Cursor cursor = new Cursor(payload);
        int id = cursor.readVarInt();
        if (cursor.failed) return payload;
        int mapped = map(id);
        if (mapped == id) return payload;

        Builder out = new Builder(payload.length + 4);
        out.writeVarInt(mapped);
        out.writeRest(payload, cursor.index);
        return out.toArray();
    }

    private byte[] rewriteLeadingInt(byte[] payload) {
        if (payload.length < 4) return payload;
        int id = ((payload[0] & 0xFF) << 24) | ((payload[1] & 0xFF) << 16)
                | ((payload[2] & 0xFF) << 8) | (payload[3] & 0xFF);
        int mapped = map(id);
        if (mapped == id) return payload;

        byte[] out = payload.clone();
        out[0] = (byte) (mapped >>> 24);
        out[1] = (byte) (mapped >>> 16);
        out[2] = (byte) (mapped >>> 8);
        out[3] = (byte) mapped;
        return out;
    }

    private byte[] rewriteVarIntList(byte[] payload, boolean leadingId) {
        Cursor cursor = new Cursor(payload);

        int leading = leadingId ? cursor.readVarInt() : 0;
        int count = cursor.readVarInt();
        if (cursor.failed || count < 0 || count > payload.length) return payload;

        int[] ids = new int[count];
        boolean changed = leadingId && map(leading) != leading;
        for (int i = 0; i < count; i++) {
            ids[i] = cursor.readVarInt();
            if (cursor.failed) return payload;
            changed |= map(ids[i]) != ids[i];
        }
        if (!changed) return payload;

        Builder out = new Builder(payload.length + 4 * (count + 1));
        if (leadingId) out.writeVarInt(map(leading));
        out.writeVarInt(count);
        for (int id : ids) out.writeVarInt(map(id));
        out.writeRest(payload, cursor.index);
        return out.toArray();
    }

    public enum Shape {

        LEADING_VARINT,
        LEADING_INT,
        VARINT_LIST,
        VEHICLE_AND_LIST
    }

    private static final class Cursor {

        private final byte[] data;
        private int index;
        private boolean failed;

        private Cursor(byte[] data) {
            this.data = data;
        }

        private int readVarInt() {
            int value = 0;
            for (int shift = 0; shift <= 28; shift += 7) {
                if (index >= data.length) {
                    failed = true;
                    return 0;
                }
                int b = data[index++] & 0xFF;
                value |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) return value;
            }
            failed = true;
            return 0;
        }
    }

    private static final class Builder {

        private byte[] data;
        private int length;

        private Builder(int capacity) {
            this.data = new byte[capacity];
        }

        private void writeVarInt(int value) {
            int remaining = value;
            while ((remaining & ~0x7F) != 0) {
                write((byte) ((remaining & 0x7F) | 0x80));
                remaining >>>= 7;
            }
            write((byte) remaining);
        }

        private void writeRest(byte[] source, int from) {
            int count = source.length - from;
            ensure(count);
            System.arraycopy(source, from, data, length, count);
            length += count;
        }

        private void write(byte value) {
            ensure(1);
            data[length++] = value;
        }

        private void ensure(int extra) {
            if (length + extra <= data.length) return;
            byte[] grown = new byte[Math.max(data.length * 2, length + extra)];
            System.arraycopy(data, 0, grown, 0, length);
            data = grown;
        }

        private byte[] toArray() {
            if (length == data.length) return data;
            byte[] exact = new byte[length];
            System.arraycopy(data, 0, exact, 0, length);
            return exact;
        }
    }
}
