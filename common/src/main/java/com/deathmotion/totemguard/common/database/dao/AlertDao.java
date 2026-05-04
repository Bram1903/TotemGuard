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
import com.deathmotion.totemguard.common.database.util.DebugTemplate;
import com.deathmotion.totemguard.common.database.util.EpochSeconds;
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
        String template = rs.getString("debug_template");
        String args = rs.getString("debug_args");
        String renderedDebug = DebugTemplate.render(template, args);
        return new AlertRecord(
                rs.getLong("id"),
                rs.getString("check_name"),
                rs.getString("server_name"),
                renderedDebug,
                rs.getString("client_brand"),
                rs.getInt("client_version"),
                EpochSeconds.toMillis(rs.getLong("created_at"))
        );
    }

    @Blocking
    public void insertBatch(List<PendingAlert> batch) throws SQLException {
        if (batch.isEmpty()) return;

        try (Connection c = connection.borrow()) {
            boolean prevAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement stmt = c.prepareStatement(Sql.INSERT_ALERT)) {
                for (PendingAlert alert : batch) {
                    stmt.setLong(1, alert.profileId());
                    stmt.setInt(2, alert.playerId());
                    stmt.setInt(3, alert.checkId());
                    if (alert.debugId() == null) stmt.setNull(4, Types.INTEGER);
                    else stmt.setInt(4, alert.debugId());
                    if (alert.debugArgs() == null) stmt.setNull(5, Types.VARCHAR);
                    else stmt.setString(5, alert.debugArgs());
                    stmt.setInt(6, alert.createdAtSeconds());
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
        return findRows(Sql.SELECT_ALERTS_BY_UUID, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
        });
    }

    @Blocking
    public int countByPlayer(UUID uuid) throws SQLException {
        return countRows(Sql.COUNT_ALERTS_BY_UUID, stmt -> stmt.setBytes(1, UuidBytes.toBytes(uuid)));
    }

    @Blocking
    public List<AlertCheckSummary> findCheckSummariesByPlayer(UUID uuid) throws SQLException {
        List<AlertCheckSummary> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_ALERT_CHECK_SUMMARIES_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    out.add(new AlertCheckSummary(rs.getString("check_name"), rs.getInt("alert_count")));
                }
            }
        }
        return out;
    }

    @Blocking
    public List<AlertRecord> findByPlayerAndCheck(UUID uuid, String checkName, int limit, int offset) throws SQLException {
        return findRows(Sql.SELECT_ALERTS_BY_UUID_CHECK, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
        });
    }

    @Blocking
    public int countByPlayerAndCheck(UUID uuid, String checkName) throws SQLException {
        return countRows(Sql.COUNT_ALERTS_BY_UUID_CHECK, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
        });
    }

    @Blocking
    public List<AlertRecord> findByPlayerSince(UUID uuid, long sinceEpochMs, int limit, int offset) throws SQLException {
        return findRows(Sql.SELECT_ALERTS_BY_UUID_SINCE, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, EpochSeconds.fromMillis(sinceEpochMs));
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
        });
    }

    @Blocking
    public int countByPlayerSince(UUID uuid, long sinceEpochMs) throws SQLException {
        return countRows(Sql.COUNT_ALERTS_BY_UUID_SINCE, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, EpochSeconds.fromMillis(sinceEpochMs));
        });
    }

    @Blocking
    public List<AlertRecord> findByPlayerAndCheckSince(UUID uuid, String checkName, long sinceEpochMs,
                                                       int limit, int offset) throws SQLException {
        return findRows(Sql.SELECT_ALERTS_BY_UUID_CHECK_SINCE, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
            stmt.setInt(3, EpochSeconds.fromMillis(sinceEpochMs));
            stmt.setInt(4, limit);
            stmt.setInt(5, offset);
        });
    }

    @Blocking
    public int countByPlayerAndCheckSince(UUID uuid, String checkName, long sinceEpochMs) throws SQLException {
        return countRows(Sql.COUNT_ALERTS_BY_UUID_CHECK_SINCE, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setString(2, checkName);
            stmt.setInt(3, EpochSeconds.fromMillis(sinceEpochMs));
        });
    }

    private List<AlertRecord> findRows(String sql, StatementBinder binder) throws SQLException {
        List<AlertRecord> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            binder.bind(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) out.add(readAlertRow(rs));
            }
        }
        return out;
    }

    private int countRows(String sql, StatementBinder binder) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            binder.bind(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement stmt) throws SQLException;
    }
}
