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

package com.deathmotion.totemguard.common.world.block;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Accessors(fluent = true)
public final class BlockStore {

    private final Map<Long, BaseChunk[]> chunks = new ConcurrentHashMap<>();
    private final ClientVersion serverBlockVersion =
            PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();

    @Getter
    @Setter
    private int minY;

    @Getter
    private int revision;

    private volatile @Nullable Watcher watcher;

    public static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) << 32 | (chunkZ & 0xFFFFFFFFL);
    }

    public static int chunkKeyX(long key) {
        return (int) (key >> 32);
    }

    public static int chunkKeyZ(long key) {
        return (int) key;
    }

    public static int stateIdIn(BaseChunk @Nullable [] sections, int minY, int x, int y, int z) {
        if (sections == null) return 0;
        int offsetY = y - minY;
        int section = offsetY >> 4;
        if (section < 0 || section >= sections.length) return 0;
        BaseChunk chunk = sections[section];
        if (chunk == null) return 0;
        return chunk.getBlockId(x & 0xF, offsetY & 0xF, z & 0xF);
    }

    public void watcher(@Nullable Watcher watcher) {
        this.watcher = watcher;
    }

    public void loadChunk(int chunkX, int chunkZ, BaseChunk[] sections) {
        long key = chunkKey(chunkX, chunkZ);
        Watcher observer = this.watcher;
        if (observer != null) observer.beforeColumn(key, chunks.get(key), false);
        chunks.put(key, sections);
        revision++;
    }

    public void mergeChunk(int chunkX, int chunkZ, BaseChunk[] sections) {
        long key = chunkKey(chunkX, chunkZ);
        BaseChunk[] existing = chunks.get(key);
        if (existing == null) return;
        Watcher observer = this.watcher;
        if (observer != null) observer.beforeColumn(key, existing.clone(), true);
        int count = Math.min(existing.length, sections.length);
        for (int i = 0; i < count; i++) {
            if (sections[i] != null) existing[i] = sections[i];
        }
        revision++;
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        Watcher observer = this.watcher;
        if (observer != null) observer.beforeColumn(key, chunks.get(key), false);
        chunks.remove(key);
        revision++;
    }

    public void clear() {
        Watcher observer = this.watcher;
        if (observer != null) observer.beforeClear();
        chunks.clear();
        revision++;
    }

    public boolean isLoaded(int chunkX, int chunkZ) {
        return chunks.containsKey(chunkKey(chunkX, chunkZ));
    }

    public Set<Long> loadedKeys() {
        return Set.copyOf(chunks.keySet());
    }

    public BaseChunk @Nullable [] sections(int chunkX, int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public BaseChunk @Nullable [] sections(long key) {
        return chunks.get(key);
    }

    BaseChunk[] columnOrNull(int chunkX, int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public int serverStateId(int x, int y, int z) {
        BaseChunk[] sections = chunks.get(chunkKey(x >> 4, z >> 4));
        if (sections == null) return 0;
        int offsetY = y - minY;
        int section = offsetY >> 4;
        if (section < 0 || section >= sections.length) return 0;
        BaseChunk chunk = sections[section];
        if (chunk == null) return 0;
        return chunk.getBlockId(x & 0xF, offsetY & 0xF, z & 0xF);
    }

    public void set(int x, int y, int z, int serverStateId) {
        BaseChunk[] sections = chunks.get(chunkKey(x >> 4, z >> 4));
        if (sections == null) return;
        int offsetY = y - minY;
        int section = offsetY >> 4;
        if (section < 0 || section >= sections.length) return;
        BaseChunk chunk = sections[section];
        if (chunk == null) {
            chunk = createSection();
            if (chunk == null) return;
            sections[section] = chunk;
        }
        Watcher observer = this.watcher;
        if (observer != null) {
            observer.beforeSet(x, y, z, chunk.getBlockId(x & 0xF, offsetY & 0xF, z & 0xF));
        }
        chunk.set(x & 0xF, offsetY & 0xF, z & 0xF, serverStateId);
    }

    private BaseChunk createSection() {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        BaseChunk chunk = version.isNewerThanOrEquals(ServerVersion.V_1_18)
                ? new Chunk_v1_18(serverBlockVersion)
                : new Chunk_v1_9(0, PaletteType.CHUNK.create());
        chunk.set(0, 0, 0, 0);
        return chunk;
    }

    public interface Watcher {

        void beforeSet(int x, int y, int z, int previousStateId);

        void beforeColumn(long key, BaseChunk @Nullable [] previous, boolean sharesSections);

        void beforeClear();
    }
}
