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

import com.deathmotion.totemguard.common.redis.binary.RedisCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public record VPNData(boolean vpn) implements RedisCodec<VPNData> {

    @Override
    public byte[] encode() throws Exception {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream)) {

            outputStream.writeBoolean(this.vpn);
            outputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
    }

    @Override
    public VPNData decode(byte[] data) throws Exception {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
             DataInputStream inputStream = new DataInputStream(byteArrayInputStream)) {

            return new VPNData(inputStream.readBoolean());
        }
    }
}