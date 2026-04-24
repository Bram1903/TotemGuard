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

public final class SessionDao {

    private final DatabaseConnectionManager connection;

    public SessionDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    @Blocking
    public long insert(
            int playerId,
            int serverId,
            String name,
            @Nullable String clientBrand,
            @Nullable Integer clientVersion,
            long startedAt
    ) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.INSERT_SESSION, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, playerId);
            stmt.setInt(2, serverId);
            stmt.setString(3, name);
            if (clientBrand == null) stmt.setNull(4, Types.VARCHAR);
            else stmt.setString(4, truncate(clientBrand, 128));
            if (clientVersion == null) stmt.setNull(5, Types.SMALLINT);
            else stmt.setInt(5, clientVersion);
            stmt.setLong(6, startedAt);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("tg_sessions insert did not return a generated key");
    }

    @Blocking
    public void markEnded(long sessionId, long endedAt) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(
                     "UPDATE tg_sessions SET ended_at = ? WHERE id = ? AND ended_at IS NULL"
             )) {
            stmt.setLong(1, endedAt);
            stmt.setLong(2, sessionId);
            stmt.executeUpdate();
        }
    }

    @Blocking
    public void closeOrphanSessions(int serverId, long now) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(
                     "UPDATE tg_sessions SET ended_at = ? WHERE server_id = ? AND ended_at IS NULL"
             )) {
            stmt.setLong(1, now);
            stmt.setInt(2, serverId);
            stmt.executeUpdate();
        }
    }
}
