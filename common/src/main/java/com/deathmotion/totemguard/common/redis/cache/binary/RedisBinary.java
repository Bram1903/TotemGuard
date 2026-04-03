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

package com.deathmotion.totemguard.common.redis.cache.binary;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class RedisBinary {

    private RedisBinary() {
    }

    public static <T> byte[] writeList(List<T> values, Writer<T> writer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeInt(values.size());
        for (T value : values) {
            writer.write(out, value);
        }

        out.flush();
        return baos.toByteArray();
    }

    public static <T> List<T> readList(byte[] data, Reader<T> reader) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));

        int count = in.readInt();
        List<T> result = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            result.add(reader.read(in));
        }

        return result;
    }

    @FunctionalInterface
    public interface Writer<T> {
        void write(DataOutput out, T value) throws IOException;
    }

    @FunctionalInterface
    public interface Reader<T> {
        T read(DataInput in) throws IOException;
    }
}
