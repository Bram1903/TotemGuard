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

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import lombok.Getter;

import java.util.Arrays;
import java.util.BitSet;

public final class ChunkLight {

    private static final int SECTION_BYTES = 2048;
    private static final int BORDER_SECTIONS = 2;

    private final byte[] emptySuffix;

    @Getter
    private final boolean spliced;
    @Getter
    private final byte[] litSuffix;
    @Getter
    private final LightData fullBright;

    public ChunkLight(ServerVersion server, int sectionCount) {
        this.spliced = server.isNewerThanOrEquals(ServerVersion.V_1_18);
        this.fullBright = fullBright(sectionCount);
        this.emptySuffix = encode(empty(), server);
        this.litSuffix = encode(fullBright, server);
    }

    private static LightData empty() {
        LightData light = new LightData();
        light.setTrustEdges(true);
        light.setSkyLightMask(new BitSet());
        light.setBlockLightMask(new BitSet());
        light.setEmptySkyLightMask(new BitSet());
        light.setEmptyBlockLightMask(new BitSet());
        light.setSkyLightCount(0);
        light.setBlockLightCount(0);
        light.setSkyLightArray(new byte[0][]);
        light.setBlockLightArray(new byte[0][]);
        return light;
    }

    private static LightData fullBright(int sectionCount) {
        int entries = Math.max(1, sectionCount) + BORDER_SECTIONS;
        byte[] lit = new byte[SECTION_BYTES];
        Arrays.fill(lit, (byte) 0xFF);

        BitSet present = new BitSet(entries);
        present.set(0, entries);
        byte[][] arrays = new byte[entries][];
        Arrays.fill(arrays, lit);

        LightData light = new LightData();
        light.setTrustEdges(true);
        light.setSkyLightMask(present);
        light.setBlockLightMask((BitSet) present.clone());
        light.setEmptySkyLightMask(new BitSet());
        light.setEmptyBlockLightMask(new BitSet());
        light.setSkyLightCount(entries);
        light.setBlockLightCount(entries);
        light.setSkyLightArray(arrays);
        light.setBlockLightArray(arrays.clone());
        return light;
    }

    private static byte[] encode(LightData data, ServerVersion server) {
        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            PacketWrapper<?> wrapper = PacketWrapper.createUniversalPacketWrapper(buffer, server);
            LightData.write(wrapper, data);
            return ByteBufHelper.copyBytes(buffer);
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    public int prefixLength(byte[] payload) {
        if (!spliced) return -1;
        int cut = payload.length - emptySuffix.length;
        if (cut < 0) return -1;
        for (int i = 0; i < emptySuffix.length; i++) {
            if (payload[cut + i] != emptySuffix[i]) return -1;
        }
        return cut;
    }
}
