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

package com.deathmotion.totemguard.common.database.dao;

import com.deathmotion.totemguard.common.database.DatabaseConnectionManager;
import com.deathmotion.totemguard.common.database.Sql;
import com.deathmotion.totemguard.common.database.model.PlayerRecord;
import com.deathmotion.totemguard.common.database.util.UuidBytes;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Upserts a player row and caches the uuid→id mapping in memory.
 */
public final class PlayerDao {

    private final DatabaseConnectionManager connection;
    private final ConcurrentMap<UUID, Integer> idCache = new ConcurrentHashMap<>();

    public PlayerDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    public void resetCache() {
        idCache.clear();
    }

    @Blocking
    public int upsertAndResolveId(UUID uuid, String name, long nowEpochMs) throws SQLException {
        Integer cached = idCache.get(uuid);
        try (Connection c = connection.borrow()) {
            try (PreparedStatement upsert = c.prepareStatement(Sql.UPSERT_PLAYER)) {
                upsert.setBytes(1, UuidBytes.toBytes(uuid));
                upsert.setString(2, name);
                upsert.setLong(3, nowEpochMs);
                upsert.setLong(4, nowEpochMs);
                upsert.executeUpdate();
            }
            if (cached != null) return cached;

            try (PreparedStatement select = c.prepareStatement(
                    "SELECT id FROM tg_players WHERE uuid = ?"
            )) {
                select.setBytes(1, UuidBytes.toBytes(uuid));
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        idCache.put(uuid, id);
                        return id;
                    }
                }
            }
        }
        throw new SQLException("Failed to resolve tg_players.id for " + uuid);
    }

    /**
     * Case-insensitive lookup by last known name. When a name has been recycled, the
     * most recently seen holder wins. Returns the canonical stored casing.
     */
    @Blocking
    public @Nullable PlayerRecord findByName(String name) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_PLAYER_BY_NAME)) {
            stmt.setString(1, name);
            return readSingle(stmt);
        }
    }

    @Blocking
    public @Nullable PlayerRecord findByUuid(UUID uuid) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_PLAYER_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            return readSingle(stmt);
        }
    }

    private @Nullable PlayerRecord readSingle(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int id = rs.getInt("id");
                UUID uuid = UuidBytes.fromBytes(rs.getBytes("uuid"));
                String canonicalName = rs.getString("last_name");
                idCache.put(uuid, id);
                return new PlayerRecord(id, uuid, canonicalName);
            }
        }
        return null;
    }
}
