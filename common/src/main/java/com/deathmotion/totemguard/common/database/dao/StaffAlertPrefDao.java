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
import com.deathmotion.totemguard.common.database.util.UuidBytes;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Durable store for staff alert-toggle preferences.
 *
 * <p>The cache in front of this handles the hot path; the DB row is the
 * source of truth across process restarts and across the 30-minute cache
 * expiry. Staff who rejoin after being offline for a day pick up exactly
 * the toggle state they left with.</p>
 */
public final class StaffAlertPrefDao {

    private final DatabaseConnectionManager connection;

    public StaffAlertPrefDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    /**
     * @return {@code null} if this UUID has never toggled alerts on this
     * network (so the caller should install a default), otherwise
     * the stored preference.
     */
    @Blocking
    public @Nullable Boolean find(UUID uuid) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_STAFF_ALERT_PREF)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getInt(1) != 0;
            }
        }
    }

    @Blocking
    public void upsert(UUID uuid, boolean enabled, long nowEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.UPSERT_STAFF_ALERT_PREF)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, enabled ? 1 : 0);
            stmt.setLong(3, nowEpochMs);
            stmt.executeUpdate();
        }
    }
}
