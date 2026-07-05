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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Until the confirming transaction returns, both the acked and the pending state are legal
// client realities.
public final class PendingBlocks {

    public static final int NONE = -1;

    private final Map<Long, Integer> pending = new ConcurrentHashMap<>();

    public static long blockKey(int x, int y, int z) {
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    public void set(int x, int y, int z, int serverStateId) {
        pending.put(blockKey(x, y, z), serverStateId);
    }

    public int peek(int x, int y, int z) {
        Integer id = pending.get(blockKey(x, y, z));
        return id == null ? NONE : id;
    }

    public void confirm(int x, int y, int z, int serverStateId) {
        long key = blockKey(x, y, z);
        Integer latest = pending.get(key);
        if (latest != null && latest == serverStateId) pending.remove(key);
    }

    public boolean has(int x, int y, int z) {
        return !pending.isEmpty() && pending.containsKey(blockKey(x, y, z));
    }

    public boolean isEmpty() {
        return pending.isEmpty();
    }

    public void clear() {
        pending.clear();
    }
}
