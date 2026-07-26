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

package com.deathmotion.totemguard.common.replay.retention;

import com.deathmotion.totemguard.common.world.block.BlockStore;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BlockJournal {

    private final Map<Long, ColumnPreImage> columns = new HashMap<>();
    private final List<BlockUndo> blocks = new ArrayList<>();

    private long sequence;
    private boolean overflowed;
    private boolean invalidated;

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 38) << 6 >> 6;
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    public boolean usable() {
        return !overflowed && !invalidated;
    }

    public boolean overflowed() {
        return overflowed;
    }

    public boolean invalidated() {
        return invalidated;
    }

    public int blockUndoCount() {
        return blocks.size();
    }

    public int columnPreImageCount() {
        return columns.size();
    }

    public synchronized void onBlockSet(int x, int y, int z, int previousStateId) {
        if (!usable()) return;
        if (blocks.size() >= RetentionPolicy.MAX_BLOCK_UNDO) {
            overflowed = true;
            return;
        }
        blocks.add(new BlockUndo(BlockStore.chunkKey(x >> 4, z >> 4), x, y, z, previousStateId, sequence++));
    }

    public synchronized void onColumn(long key, BaseChunk @Nullable [] previous, boolean sharesSections) {
        if (!usable()) return;
        if (columns.containsKey(key)) {
            sequence++;
            return;
        }
        if (columns.size() >= RetentionPolicy.MAX_COLUMN_PRE_IMAGES) {
            overflowed = true;
            return;
        }
        columns.put(key, new ColumnPreImage(previous, sequence++, sharesSections));
    }

    public synchronized void onClear() {
        invalidated = true;
        columns.clear();
        blocks.clear();
    }

    public synchronized Rewind rewind(BlockStore store) {
        Map<Long, BaseChunk[]> base = new HashMap<>();
        for (long key : store.loadedKeys()) {
            ColumnPreImage pre = columns.get(key);
            if (pre == null) {
                base.put(key, store.sections(key));
            } else if (pre.sections != null) {
                base.put(key, pre.sections);
            }
        }
        for (Map.Entry<Long, ColumnPreImage> entry : columns.entrySet()) {
            ColumnPreImage pre = entry.getValue();
            if (pre.sections != null && !base.containsKey(entry.getKey())) {
                base.put(entry.getKey(), pre.sections);
            }
        }

        Map<Long, Integer> undo = new HashMap<>();
        for (BlockUndo entry : blocks) {
            if (!applies(entry)) continue;
            undo.putIfAbsent(pack(entry.x, entry.y, entry.z), entry.previousStateId);
        }
        return new Rewind(base, undo, store.minY());
    }

    private boolean applies(BlockUndo entry) {
        ColumnPreImage pre = columns.get(entry.columnKey);
        if (pre == null) return true;
        if (pre.sections == null) return false;
        return pre.sharesSections || entry.sequence < pre.sequence;
    }

    public record BlockUndo(long columnKey, int x, int y, int z, int previousStateId, long sequence) {
    }

    private record ColumnPreImage(BaseChunk @Nullable [] sections, long sequence, boolean sharesSections) {
    }

    public static final class Rewind {

        private final Map<Long, BaseChunk[]> columns;
        private final Map<Long, Integer> blockUndo;
        private final int minY;

        Rewind(Map<Long, BaseChunk[]> columns, Map<Long, Integer> blockUndo, int minY) {
            this.columns = columns;
            this.blockUndo = blockUndo;
            this.minY = minY;
        }

        public Map<Long, BaseChunk[]> columns() {
            return columns;
        }

        public Map<Long, Integer> blockUndo() {
            return blockUndo;
        }

        public int minY() {
            return minY;
        }

        public boolean isLoaded(int chunkX, int chunkZ) {
            return columns.containsKey(BlockStore.chunkKey(chunkX, chunkZ));
        }

        public int stateId(int x, int y, int z) {
            Integer restored = blockUndo.get(pack(x, y, z));
            if (restored != null) return restored;
            return BlockStore.stateIdIn(columns.get(BlockStore.chunkKey(x >> 4, z >> 4)), minY, x, y, z);
        }
    }
}
