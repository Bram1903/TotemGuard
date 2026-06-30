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

package com.deathmotion.totemguard.common.player.data;

import com.deathmotion.totemguard.common.util.BoundingBox;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class ClientWorld {

    private final Map<Long, BaseChunk[]> chunks = new ConcurrentHashMap<>();
    private final ClientVersion blockVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
    private final WrappedBlockState air = WrappedBlockState.getByGlobalId(blockVersion, 0, false);

    private int minHeight = 0;

    private static long key(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) << 32 | (chunkZ & 0xFFFFFFFFL);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    public void setDimension(int minY) {
        this.minHeight = minY;
    }

    public void loadChunk(int chunkX, int chunkZ, BaseChunk[] sections) {
        chunks.put(key(chunkX, chunkZ), sections);
    }

    public void mergeChunk(int chunkX, int chunkZ, BaseChunk[] sections) {
        BaseChunk[] existing = chunks.get(key(chunkX, chunkZ));
        if (existing == null) return;
        int count = Math.min(existing.length, sections.length);
        for (int i = 0; i < count; i++) {
            if (sections[i] != null) existing[i] = sections[i];
        }
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        chunks.remove(key(chunkX, chunkZ));
    }

    public void clear() {
        chunks.clear();
    }

    public boolean isLoaded(int chunkX, int chunkZ) {
        return chunks.containsKey(key(chunkX, chunkZ));
    }

    public WrappedBlockState getBlockState(int x, int y, int z) {
        BaseChunk[] sections = chunks.get(key(x >> 4, z >> 4));
        if (sections == null) return air;

        int offsetY = y - minHeight;
        int section = offsetY >> 4;
        if (section < 0 || section >= sections.length) return air;

        BaseChunk chunk = sections[section];
        if (chunk == null) return air;

        return chunk.get(blockVersion, x & 0xF, offsetY & 0xF, z & 0xF);
    }

    public boolean hasBlock(BoundingBox box, Predicate<WrappedBlockState> test) {
        return forEachBlock(box, test::test);
    }

    public boolean forEachBlock(BoundingBox box, BlockVisitor visitor) {
        int x0 = floor(box.minX()), x1 = floor(box.maxX());
        int y0 = floor(box.minY()), y1 = floor(box.maxY());
        int z0 = floor(box.minZ()), z1 = floor(box.maxZ());
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (visitor.visit(getBlockState(x, y, z))) return true;
                }
            }
        }
        return false;
    }

    public boolean containsNetherPortal(BoundingBox box) {
        return hasBlock(box, state -> state.getType() == StateTypes.NETHER_PORTAL);
    }

    public int getBlockId(int x, int y, int z) {
        BaseChunk[] sections = chunks.get(key(x >> 4, z >> 4));
        if (sections == null) return 0;

        int offsetY = y - minHeight;
        int section = offsetY >> 4;
        if (section < 0 || section >= sections.length) return 0;

        BaseChunk chunk = sections[section];
        if (chunk == null) return 0;

        return chunk.getBlockId(x & 0xF, offsetY & 0xF, z & 0xF);
    }

    public void updateBlock(int x, int y, int z, int blockId) {
        BaseChunk[] sections = chunks.get(key(x >> 4, z >> 4));
        if (sections == null) return;

        int offsetY = y - minHeight;
        int section = offsetY >> 4;
        if (section < 0 || section >= sections.length) return;

        BaseChunk chunk = sections[section];
        if (chunk == null) {
            chunk = createSection();
            if (chunk == null) return;
            sections[section] = chunk;
        }
        chunk.set(x & 0xF, offsetY & 0xF, z & 0xF, blockId);
    }

    private BaseChunk createSection() {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (version.isNewerThanOrEquals(ServerVersion.V_1_18)) {
            return new Chunk_v1_18(blockVersion);
        }
        if (version.isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return new Chunk_v1_9(0, PaletteType.CHUNK.create());
        }
        return null;
    }

    @FunctionalInterface
    public interface BlockVisitor {
        boolean visit(WrappedBlockState state);
    }
}
