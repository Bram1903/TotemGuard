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

package com.deathmotion.totemguard.common.cache.data;

import com.deathmotion.totemguard.common.redis.binary.RedisBinary;
import com.deathmotion.totemguard.common.redis.binary.RedisCodec;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public record CheckSnapshot(String checkName, double buffer, int violations) implements RedisCodec<CheckSnapshot> {

    static CheckSnapshot read(DataInput in) throws IOException {
        int len = in.readInt();
        byte[] name = new byte[len];
        in.readFully(name);

        return new CheckSnapshot(
                new String(name, StandardCharsets.UTF_8),
                in.readDouble(),
                in.readInt()
        );
    }

    public static byte[] encodeList(List<CheckSnapshot> snapshots) throws IOException {
        return RedisBinary.writeList(snapshots, (out, s) -> s.write(out));
    }

    public static List<CheckSnapshot> decodeList(byte[] data) throws IOException {
        return RedisBinary.readList(data, CheckSnapshot::read);
    }

    @Override
    public byte[] encode() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        write(new DataOutputStream(baos));
        return baos.toByteArray();
    }

    @Override
    public CheckSnapshot decode(byte[] data) throws IOException {
        return read(new DataInputStream(new ByteArrayInputStream(data)));
    }

    void write(DataOutput out) throws IOException {
        byte[] name = checkName.getBytes(StandardCharsets.UTF_8);
        out.writeInt(name.length);
        out.write(name);
        out.writeDouble(buffer);
        out.writeInt(violations);
    }
}