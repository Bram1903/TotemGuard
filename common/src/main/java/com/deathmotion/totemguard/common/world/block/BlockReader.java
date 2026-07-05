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

import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class BlockReader {

    private final BlockStore store;
    private final PendingBlocks pending;
    private final ClientStateMap stateMap;
    private final StateFacts factsTable;
    private final WrappedBlockState air;

    private long memoKey = Long.MIN_VALUE;
    private BaseChunk[] memoColumn;
    private int memoRevision = -1;

    @Getter
    private int readsThisTick;
    @Getter
    private int missesThisTick;
    @Getter
    private int uncertainHitsThisTick;

    public BlockReader(BlockStore store, PendingBlocks pending, ClientStateMap stateMap) {
        this.store = store;
        this.pending = pending;
        this.stateMap = stateMap;
        this.factsTable = StateFacts.tableFor(stateMap.stateVersion());
        this.air = WrappedBlockState.getByGlobalId(stateMap.stateVersion(), 0, false);
    }

    public ClientStateMap stateMap() {
        return stateMap;
    }

    public int stateId(int x, int y, int z) {
        return stateMap.toClientId(serverStateId(x, y, z));
    }

    public WrappedBlockState state(int x, int y, int z) {
        int clientId = stateId(x, y, z);
        if (clientId == 0) return air;
        return WrappedBlockState.getByGlobalId(stateMap.stateVersion(), clientId, false);
    }

    public WrappedBlockState stateForClientId(int clientId) {
        if (clientId == 0) return air;
        return WrappedBlockState.getByGlobalId(stateMap.stateVersion(), clientId, false);
    }

    public long facts(int x, int y, int z) {
        return factsTable.of(stateId(x, y, z));
    }

    public long factsForClientId(int clientId) {
        return factsTable.of(clientId);
    }

    public boolean columnLoaded(int chunkX, int chunkZ) {
        return store.isLoaded(chunkX, chunkZ);
    }

    public boolean uncertain(int x, int y, int z) {
        boolean hit = pending.has(x, y, z);
        if (hit) uncertainHitsThisTick++;
        return hit;
    }

    public int pendingStateId(int x, int y, int z) {
        int serverId = pending.peek(x, y, z);
        return serverId == PendingBlocks.NONE ? PendingBlocks.NONE : stateMap.toClientId(serverId);
    }

    public boolean containsPortal(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int x0 = floor(minX), x1 = floor(maxX);
        int y0 = floor(minY), y1 = floor(maxY);
        int z0 = floor(minZ), z1 = floor(maxZ);
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (StateFacts.is(facts(x, y, z), StateFacts.NETHER_PORTAL)) return true;
                }
            }
        }
        return false;
    }

    public void resetCounters() {
        readsThisTick = 0;
        missesThisTick = 0;
        uncertainHitsThisTick = 0;
    }

    private int serverStateId(int x, int y, int z) {
        readsThisTick++;
        long key = BlockStore.chunkKey(x >> 4, z >> 4);
        if (key != memoKey || memoRevision != store.revision()) {
            memoColumn = store.columnOrNull(x >> 4, z >> 4);
            memoKey = key;
            memoRevision = store.revision();
        }
        BaseChunk[] sections = memoColumn;
        if (sections == null) {
            missesThisTick++;
            return 0;
        }
        int offsetY = y - store.minY();
        int section = offsetY >> 4;
        if (section < 0 || section >= sections.length) return 0;
        BaseChunk chunk = sections[section];
        if (chunk == null) return 0;
        return chunk.getBlockId(x & 0xF, offsetY & 0xF, z & 0xF);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
