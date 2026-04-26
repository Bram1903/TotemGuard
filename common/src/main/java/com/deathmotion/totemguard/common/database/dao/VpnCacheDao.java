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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class VpnCacheDao {

    private static final int PROVIDER_MAX_LENGTH = 64;

    private final DatabaseConnectionManager connection;

    public VpnCacheDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    @Blocking
    public @Nullable Boolean find(byte @NotNull [] ipHash, long freshAfterEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_VPN_CACHE)) {
            stmt.setBytes(1, ipHash);
            stmt.setLong(2, freshAfterEpochMs);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt(1) != 0;
            }
        }
    }

    @Blocking
    public void upsert(byte @NotNull [] ipHash,
                       boolean isVpn,
                       @NotNull String provider,
                       long cachedAtEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.UPSERT_VPN_CACHE)) {
            stmt.setBytes(1, ipHash);
            stmt.setInt(2, isVpn ? 1 : 0);
            stmt.setString(3, truncate(provider, PROVIDER_MAX_LENGTH));
            stmt.setLong(4, cachedAtEpochMs);
            stmt.executeUpdate();
        }
    }
}
