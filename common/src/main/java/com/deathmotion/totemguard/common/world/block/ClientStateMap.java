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
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Accessors(fluent = true)
public final class ClientStateMap {

    private static final boolean VIA_AVAILABLE = hasClass("com.viaversion.viaversion.api.Via");
    private static final int MAX_CACHED_STATE_ID = 1 << 16;
    private static final int UNCOMPUTED = Integer.MIN_VALUE;

    private static final Map<Integer, ClientStateMap> CACHE = new ConcurrentHashMap<>();

    @Getter
    private final ClientVersion clientVersion;
    @Getter
    private final ClientVersion stateVersion;
    @Getter
    private final boolean identity;

    private final int clientProtocol;
    private final int serverProtocol;
    private final int[] table;

    private ClientStateMap(ClientVersion clientVersion) {
        ClientVersion serverBlockVersion = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        this.clientVersion = clientVersion;
        this.clientProtocol = clientVersion.getProtocolVersion();
        this.serverProtocol = serverBlockVersion.getProtocolVersion();
        this.identity = !VIA_AVAILABLE || clientProtocol >= serverProtocol;
        this.stateVersion = identity ? serverBlockVersion : clientVersion;
        if (identity) {
            this.table = null;
        } else {
            this.table = new int[MAX_CACHED_STATE_ID];
            Arrays.fill(table, UNCOMPUTED);
        }
    }

    public static ClientStateMap forClient(ClientVersion clientVersion) {
        return CACHE.computeIfAbsent(clientVersion.getProtocolVersion(), p -> new ClientStateMap(clientVersion));
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public int toClientId(int serverStateId) {
        if (identity || serverStateId < 0) return serverStateId;
        if (serverStateId >= MAX_CACHED_STATE_ID) return translate(serverStateId);
        int cached = table[serverStateId];
        if (cached != UNCOMPUTED) return cached;
        int resolved = translate(serverStateId);
        table[serverStateId] = resolved;
        return resolved;
    }

    private int translate(int serverStateId) {
        try {
            List<ProtocolPathEntry> path = Via.getManager().getProtocolManager()
                    .getProtocolPath(ProtocolVersion.getProtocol(clientProtocol), ProtocolVersion.getProtocol(serverProtocol));
            if (path == null) return serverStateId;
            int id = serverStateId;
            for (int i = path.size() - 1; i >= 0; i--) {
                Protocol<?, ?, ?, ?> protocol = path.get(i).protocol();
                MappingData mappingData = protocol.getMappingData();
                if (mappingData != null && mappingData.getBlockStateMappings() != null) {
                    id = mappingData.getNewBlockStateId(id);
                }
            }
            return id;
        } catch (Throwable t) {
            return serverStateId;
        }
    }
}
