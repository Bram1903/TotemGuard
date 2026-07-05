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

package com.deathmotion.totemguard.common.util;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ViaBlockTranslator {

    private static final boolean AVAILABLE = hasClass("com.viaversion.viaversion.api.Via");

    private static final int MAX_CACHED_BLOCK_ID = 1 << 16;
    private static final int UNCOMPUTED = Integer.MIN_VALUE;

    private static final Map<Integer, int[]> CACHE = new ConcurrentHashMap<>();

    private ViaBlockTranslator() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static int toClientBlockId(int clientProtocol, int serverProtocol, int serverBlockId) {
        if (!AVAILABLE || clientProtocol >= serverProtocol || serverBlockId < 0) {
            return serverBlockId;
        }
        if (serverBlockId >= MAX_CACHED_BLOCK_ID) {
            return translate(clientProtocol, serverProtocol, serverBlockId);
        }

        int[] table = CACHE.computeIfAbsent(clientProtocol, k -> newTable());
        int cached = table[serverBlockId];
        if (cached != UNCOMPUTED) {
            return cached;
        }
        int resolved = translate(clientProtocol, serverProtocol, serverBlockId);
        table[serverBlockId] = resolved;
        return resolved;
    }

    private static int translate(int clientProtocol, int serverProtocol, int serverBlockId) {
        try {
            List<ProtocolPathEntry> path = Via.getManager().getProtocolManager()
                    .getProtocolPath(ProtocolVersion.getProtocol(clientProtocol), ProtocolVersion.getProtocol(serverProtocol));
            if (path == null) {
                return serverBlockId;
            }
            int id = serverBlockId;
            for (int i = path.size() - 1; i >= 0; i--) {
                Protocol<?, ?, ?, ?> protocol = path.get(i).protocol();
                MappingData mappingData = protocol.getMappingData();
                if (mappingData != null && mappingData.getBlockStateMappings() != null) {
                    id = mappingData.getNewBlockStateId(id);
                }
            }
            return id;
        } catch (Throwable t) {
            return serverBlockId;
        }
    }

    private static int[] newTable() {
        int[] table = new int[MAX_CACHED_BLOCK_ID];
        java.util.Arrays.fill(table, UNCOMPUTED);
        return table;
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
