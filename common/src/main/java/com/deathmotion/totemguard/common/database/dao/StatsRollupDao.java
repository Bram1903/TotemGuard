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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class StatsRollupDao {

    private final DatabaseConnectionManager connection;

    public StatsRollupDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    @Blocking
    public void incrementAlerts(int dayEpoch, int delta) throws SQLException {
        if (delta <= 0) return;
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.UPSERT_STATS_DAILY_ALERTS)) {
            stmt.setInt(1, dayEpoch);
            stmt.setInt(2, delta);
            stmt.executeUpdate();
        }
    }

    @Blocking
    public void incrementPunishments(int dayEpoch, int delta) throws SQLException {
        if (delta <= 0) return;
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.UPSERT_STATS_DAILY_PUNISHMENTS)) {
            stmt.setInt(1, dayEpoch);
            stmt.setInt(2, delta);
            stmt.executeUpdate();
        }
    }

    @Blocking
    public Totals sumAllTime() throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SUM_STATS_DAILY_ALL_TIME);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return new Totals(rs.getLong(1), rs.getLong(2));
            return Totals.ZERO;
        }
    }

    @Blocking
    public Totals sumSince(long sinceEpochMs) throws SQLException {
        int day = EpochSeconds.dayFromMillis(sinceEpochMs);
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SUM_STATS_DAILY_SINCE)) {
            stmt.setInt(1, day);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return new Totals(rs.getLong(1), rs.getLong(2));
                return Totals.ZERO;
            }
        }
    }

    public record Totals(long alerts, long punishments) {
        public static final Totals ZERO = new Totals(0L, 0L);
    }
}
