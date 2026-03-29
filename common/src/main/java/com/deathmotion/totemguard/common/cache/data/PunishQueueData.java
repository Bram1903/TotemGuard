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

import com.deathmotion.totemguard.common.redis.cache.binary.RedisCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public record PunishQueueData(long createdAt) implements RedisCodec<PunishQueueData> {

    public static PunishQueueData now() {
        return new PunishQueueData(System.currentTimeMillis());
    }

    @Override
    public byte[] encode() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(Long.BYTES);
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeLong(createdAt);
        }
        return baos.toByteArray();
    }

    @Override
    public PunishQueueData decode(byte[] data) throws IOException {
        if (data.length == 0) {
            return new PunishQueueData(0L);
        }

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            return new PunishQueueData(in.readLong());
        }
    }
}
