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
import com.deathmotion.totemguard.common.database.model.AlertCheckSummary;
import com.deathmotion.totemguard.common.database.model.AlertRecord;
import com.deathmotion.totemguard.common.database.model.PendingAlert;
import com.deathmotion.totemguard.common.database.util.UuidBytes;
import org.jetbrains.annotations.Blocking;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AlertDao {

    private final DatabaseConnectionManager connection;

    public AlertDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    private static AlertRecord readAlertRow(ResultSet rs) throws SQLException {
        return new AlertRecord(
                rs.getLong("id"),
                rs.getString("check_name"),
                rs.getString("server_name"),
                rs.getInt("violations"),
                rs.getString("debug"),
                readNullableInt(rs, "keepalive_ping"),
                readNullableInt(rs, "transaction_ping"),
                rs.getString("client_brand"),
                readNullableInt(rs, "client_version"),
                rs.getLong("created_at")
        );
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    private static void setPing(PreparedStatement stmt, int index, Integer value) throws SQLException {
        if (value == null || value < 0) {
            stmt.setNull(index, Types.SMALLINT);
            return;
        }
        stmt.setInt(index, Math.min(value, 65_535));
    }

    private static Integer readNullableInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    @Blocking
    public void insertBatch(List<PendingAlert> batch) throws SQLException {
        if (batch.isEmpty()) return;

        try (Connection c = connection.borrow()) {
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement stmt = c.prepareStatement(Sql.INSERT_ALERT)) {
                for (PendingAlert alert : batch) {
                    if (alert.profileId() == null) stmt.setNull(1, Types.BIGINT);
                    else stmt.setLong(1, alert.profileId());
                    stmt.setInt(2, alert.playerId());
                    stmt.setInt(3, alert.serverId());
                    stmt.setInt(4, alert.checkId());
                    stmt.setLong(5, alert.violations());
                    if (alert.debug() == null) stmt.setNull(6, Types.VARCHAR);
                    else stmt.setString(6, truncate(alert.debug(), 128));
                    setPing(stmt, 7, alert.keepalivePing());
                    setPing(stmt, 8, alert.transactionPing());
                    stmt.setLong(9, alert.createdAt());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(prevAutoCommit);
            }
        }
    }

    @Blocking
    public long deleteOlderThan(long cutoffEpochMs, int chunkSize) throws SQLException {
        return deleteOldChunked(Sql.DELETE_OLD_ALERTS, cutoffEpochMs, chunkSize);
    }

    @Blocking
    public long deleteOldVpnCacheEntries(long cutoffEpochMs, int chunkSize) throws SQLException {
        return deleteOldChunked(Sql.DELETE_OLD_VPN_CACHE, cutoffEpochMs, chunkSize);
    }

    @Blocking
    public long deleteByPlayer(UUID uuid, int chunkSize) throws SQLException {
        long total = 0;
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.DELETE_ALERTS_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, chunkSize);
            while (true) {
                int removed = stmt.executeUpdate();
                total += removed;
                if (removed < chunkSize) break;
            }
        }
        return total;
    }

    @Blocking
    public List<AlertRecord> findByPlayer(UUID uuid, int limit, int offset) throws SQLException {
        List<AlertRecord> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERTS_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) out.add(readAlertRow(rs));
            }
        }
        return out;
    }

    @Blocking
    public int countByPlayer(UUID uuid) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_ALERTS_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Blocking
    public List<AlertCheckSummary> findCheckSummariesByPlayer(UUID uuid) throws SQLException {
        List<AlertCheckSummary> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERT_CHECK_SUMMARIES_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    out.add(new AlertCheckSummary(
                            rs.getString("check_name"),
                            rs.getInt("alert_count")
                    ));
                }
            }
        }
        return out;
    }

    @Blocking
    public List<AlertRecord> findByPlayerAndCheck(UUID uuid, String checkName, int limit, int offset) throws SQLException {
        List<AlertRecord> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERTS_BY_UUID_CHECK)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) out.add(readAlertRow(rs));
            }
        }
        return out;
    }

    @Blocking
    public int countByPlayerAndCheck(UUID uuid, String checkName) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_ALERTS_BY_UUID_CHECK)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Blocking
    public List<AlertRecord> findByPlayerSince(UUID uuid, long sinceEpochMs, int limit, int offset) throws SQLException {
        List<AlertRecord> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERTS_BY_UUID_SINCE)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setLong(2, sinceEpochMs);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) out.add(readAlertRow(rs));
            }
        }
        return out;
    }

    @Blocking
    public int countByPlayerSince(UUID uuid, long sinceEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_ALERTS_BY_UUID_SINCE)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setLong(2, sinceEpochMs);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Blocking
    public List<AlertRecord> findByPlayerAndCheckSince(UUID uuid, String checkName, long sinceEpochMs, int limit, int offset) throws SQLException {
        List<AlertRecord> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERTS_BY_UUID_CHECK_SINCE)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
            stmt.setLong(3, sinceEpochMs);
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) out.add(readAlertRow(rs));
            }
        }
        return out;
    }

    @Blocking
    public int countByPlayerAndCheckSince(UUID uuid, String checkName, long sinceEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_ALERTS_BY_UUID_CHECK_SINCE)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
            stmt.setLong(3, sinceEpochMs);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Blocking
    public int countSince(long sinceEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_ALERTS_SINCE)) {
            stmt.setLong(1, sinceEpochMs);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private long deleteOldChunked(String sql, long cutoffEpochMs, int chunkSize) throws SQLException {
        long total = 0;
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            stmt.setLong(1, cutoffEpochMs);
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
