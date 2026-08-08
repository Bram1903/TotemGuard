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

public final class ProfileDao {

    private final DatabaseConnectionManager connection;

    public ProfileDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    @Blocking
    public long resolveOrCreate(int playerId, int serverId, int brandId, int clientVersion) throws SQLException {
        try (Connection c = connection.borrow()) {
            Long existing = findExisting(c, playerId, serverId, brandId, clientVersion);
            if (existing != null) return existing;

            try (PreparedStatement insert = c.prepareStatement(Sql.INSERT_PROFILE, Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, playerId);
                insert.setInt(2, serverId);
                insert.setInt(3, brandId);
                insert.setInt(4, clientVersion);
                try {
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (keys.next()) return keys.getLong(1);
                    }
                } catch (SQLException ex) {
                    Long raced = findExisting(c, playerId, serverId, brandId, clientVersion);
                    if (raced != null) return raced;
                    throw ex;
                }
            }
        }
        throw new SQLException("tg_profiles insert did not return a generated key");
    }

    private @Nullable Long findExisting(Connection c, int playerId, int serverId, int brandId, int clientVersion) throws SQLException {
        try (PreparedStatement lookup = c.prepareStatement(Sql.SELECT_PROFILE_ID)) {
            lookup.setInt(1, playerId);
            lookup.setInt(2, serverId);
            lookup.setInt(3, brandId);
            lookup.setInt(4, clientVersion);
            try (ResultSet rs = lookup.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return null;
    }
}
