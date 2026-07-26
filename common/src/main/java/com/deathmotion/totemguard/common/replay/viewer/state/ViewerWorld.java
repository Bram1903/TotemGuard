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

package com.deathmotion.totemguard.common.replay.viewer.state;

import com.deathmotion.totemguard.common.replay.retention.BlockJournal;
import com.deathmotion.totemguard.common.world.block.BlockStore;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ViewerWorld {

    private static final int MAX_TAB_ENTRIES = 512;

    private final Map<Long, ViewerColumn> columns = new LinkedHashMap<>();
    private final Map<Integer, ViewerEntity> entities = new LinkedHashMap<>();
    private final List<RetainedPacket> tabList = new ArrayList<>();
    private final Set<UUID> profiles = new LinkedHashSet<>();
    private final Map<String, RetainedPacket> teams = new LinkedHashMap<>();
    private final Map<Integer, RetainedPacket> border = new LinkedHashMap<>();

    public static long columnKeyOf(byte[] chunkPayload) {
        if (chunkPayload.length < 8) return Long.MIN_VALUE;
        return BlockStore.chunkKey(readInt(chunkPayload, 0), readInt(chunkPayload, 4));
    }

    private static int readInt(byte[] data, int at) {
        return ((data[at] & 0xFF) << 24) | ((data[at + 1] & 0xFF) << 16)
                | ((data[at + 2] & 0xFF) << 8) | (data[at + 3] & 0xFF);
    }

    public void clear() {
        columns.clear();
        entities.clear();
        tabList.clear();
        profiles.clear();
        teams.clear();
        border.clear();
    }

    public void chunk(int packetId, byte[] payload) {
        long key = columnKeyOf(payload);
        if (key == Long.MIN_VALUE) return;
        columns.put(key, new ViewerColumn(new RetainedPacket(packetId, payload)));
    }

    public void unloadChunk(int chunkX, int chunkZ) {
        columns.remove(BlockStore.chunkKey(chunkX, chunkZ));
    }

    public void blockEdit(int x, int y, int z, int state) {
        ViewerColumn column = columns.get(BlockStore.chunkKey(x >> 4, z >> 4));
        if (column == null) return;
        column.edit(BlockJournal.pack(x, y, z), state);
    }

    public Collection<ViewerColumn> columns() {
        return columns.values();
    }

    public Set<Long> columnKeys() {
        return columns.keySet();
    }

    public Map<Long, ViewerColumn> columnMap() {
        return columns;
    }

    public void spawn(int entityId, int packetId, byte[] payload) {
        entities.put(entityId, new ViewerEntity(new RetainedPacket(packetId, payload)));
    }

    public void destroy(int entityId) {
        entities.remove(entityId);
    }

    public @Nullable ViewerEntity entity(int entityId) {
        return entities.get(entityId);
    }

    public Map<Integer, ViewerEntity> entities() {
        return entities;
    }

    public void addProfiles(int packetId, byte[] payload, Collection<UUID> added) {
        if (tabList.size() >= MAX_TAB_ENTRIES) return;
        tabList.add(new RetainedPacket(packetId, payload));
        profiles.addAll(added);
    }

    public void removeProfiles(Collection<UUID> removed) {
        profiles.removeAll(removed);
    }

    public List<RetainedPacket> tabList() {
        return tabList;
    }

    public Set<UUID> profiles() {
        return profiles;
    }

    public void team(String name, @Nullable RetainedPacket packet) {
        if (packet == null) {
            teams.remove(name);
            return;
        }
        if (teams.size() >= MAX_TAB_ENTRIES && !teams.containsKey(name)) return;
        teams.put(name, packet);
    }

    public Collection<RetainedPacket> teams() {
        return teams.values();
    }

    public void border(int packetId, byte[] payload) {
        border.put(packetId, new RetainedPacket(packetId, payload));
    }

    public Collection<RetainedPacket> border() {
        return border.values();
    }
}
