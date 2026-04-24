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
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.sql.*;

/**
 * Reuses existing rows when <player, server, brand, version> already exists;
 * otherwise inserts a fresh one.
 */
public final class ProfileDao {

    private final DatabaseConnectionManager connection;

    public ProfileDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    @Blocking
    public long resolveOrCreate(
            int playerId,
            int serverId,
            @Nullable String clientBrand,
            @Nullable Integer clientVersion
    ) throws SQLException {
        String brand = clientBrand == null ? null : truncate(clientBrand, 64);

        try (Connection c = connection.borrow()) {
            Long existing = findExisting(c, playerId, serverId, brand, clientVersion);
            if (existing != null) return existing;

            try (PreparedStatement insert = c.prepareStatement(Sql.INSERT_PROFILE, Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, playerId);
                insert.setInt(2, serverId);
                if (brand == null) insert.setNull(3, Types.VARCHAR);
                else insert.setString(3, brand);
                if (clientVersion == null) insert.setNull(4, Types.SMALLINT);
                else insert.setInt(4, clientVersion);
                try {
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (keys.next()) return keys.getLong(1);
                    }
                } catch (SQLException ex) {
                    // Two servers may race on the same profile tuple. Fall back to a lookup.
                    Long raced = findExisting(c, playerId, serverId, brand, clientVersion);
                    if (raced != null) return raced;
                    throw ex;
                }
            }
        }
        throw new SQLException("tg_profiles insert did not return a generated key");
    }

    private @Nullable Long findExisting(
            Connection c,
            int playerId,
            int serverId,
            @Nullable String brand,
            @Nullable Integer clientVersion
    ) throws SQLException {
        try (PreparedStatement lookup = c.prepareStatement(Sql.SELECT_PROFILE_ID)) {
            lookup.setInt(1, playerId);
            lookup.setInt(2, serverId);
            if (brand == null) lookup.setNull(3, Types.VARCHAR);
            else lookup.setString(3, brand);
            if (clientVersion == null) lookup.setNull(4, Types.SMALLINT);
            else lookup.setInt(4, clientVersion);
            try (ResultSet rs = lookup.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return null;
    }
}
