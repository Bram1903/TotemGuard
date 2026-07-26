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

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.buffer.UnpooledByteBufAllocationHelper;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.HeightmapType;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.Collections;
import java.util.Map;

public final class ChunkPayloadCompactor {

    private static final TileEntity[] NO_TILE_ENTITIES = new TileEntity[0];
    private static final Map<HeightmapType, long[]> NO_HEIGHTMAPS = Collections.emptyMap();

    private ChunkPayloadCompactor() {
    }

    public static @Nullable byte[] compact(PacketSendEvent event, ServerVersion server) {
        if (server.isOlderThan(ServerVersion.V_1_18)) return null;
        try {
            return encode(new WrapperPlayServerChunkData(event).getColumn(), server);
        } catch (RuntimeException failure) {
            return null;
        }
    }

    public static @Nullable byte[] encode(@Nullable Column source, ServerVersion server) {
        if (source == null) return null;
        Column stripped = new Column(source.getX(), source.getZ(), source.isFullChunk(),
                source.getChunks(), NO_TILE_ENTITIES, NO_HEIGHTMAPS);

        Object buffer = UnpooledByteBufAllocationHelper.buffer();
        try {
            WrapperPlayServerChunkData compacted = new WrapperPlayServerChunkData(stripped, darkness());
            compacted.setServerVersion(server);
            compacted.setBuffer(buffer);
            compacted.write();
            return ByteBufHelper.copyBytes(buffer);
        } finally {
            ByteBufHelper.release(buffer);
        }
    }

    private static LightData darkness() {
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
}
