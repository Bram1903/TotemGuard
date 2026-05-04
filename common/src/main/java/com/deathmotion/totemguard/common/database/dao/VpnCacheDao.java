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
import com.deathmotion.totemguard.common.database.util.EpochSeconds;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class VpnCacheDao {

    private final DatabaseConnectionManager connection;

    public VpnCacheDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    @Blocking
    public @Nullable Boolean find(byte @NotNull [] ipHash, long freshAfterEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_VPN_CACHE)) {
            stmt.setBytes(1, ipHash);
            stmt.setInt(2, EpochSeconds.fromMillis(freshAfterEpochMs));
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt(1) != 0;
            }
        }
    }

    @Blocking
    public void upsert(byte @NotNull [] ipHash, boolean isVpn, long cachedAtEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.UPSERT_VPN_CACHE)) {
            stmt.setBytes(1, ipHash);
            stmt.setInt(2, isVpn ? 1 : 0);
            stmt.setInt(3, EpochSeconds.fromMillis(cachedAtEpochMs));
            stmt.executeUpdate();
        }
    }

    @Blocking
    public long deleteOlderThan(long cutoffEpochMs, int chunkSize) throws SQLException {
        long total = 0;
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.DELETE_OLD_VPN_CACHE)) {
            stmt.setInt(1, EpochSeconds.fromMillis(cutoffEpochMs));
            stmt.setInt(2, chunkSize);
            while (true) {
                int removed = stmt.executeUpdate();
                total += removed;
                if (removed < chunkSize) break;
            }
        }
        return total;
    }
}
