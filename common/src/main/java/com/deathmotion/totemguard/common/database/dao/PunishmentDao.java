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

import com.deathmotion.totemguard.api.punishment.PunishmentType;
import com.deathmotion.totemguard.common.database.DatabaseConnectionManager;
import com.deathmotion.totemguard.common.database.Sql;
import com.deathmotion.totemguard.common.database.model.PunishmentRecord;
import com.deathmotion.totemguard.common.database.util.DebugTemplate;
import com.deathmotion.totemguard.common.database.util.EpochSeconds;
import com.deathmotion.totemguard.common.database.util.UuidBytes;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PunishmentDao {

    private final DatabaseConnectionManager connection;

    public PunishmentDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    private static PunishmentRecord readRow(ResultSet rs, PunishmentType[] types) throws SQLException {
        int ordinal = rs.getInt("type");
        PunishmentType type = (ordinal >= 0 && ordinal < types.length) ? types[ordinal] : PunishmentType.GENERIC;
        String renderedCommand = DebugTemplate.render(rs.getString("command_template"), rs.getString("command_args"));
        String renderedDebug = DebugTemplate.render(rs.getString("debug_template"), rs.getString("debug_args"));
        return new PunishmentRecord(
                rs.getLong("id"),
                rs.getString("check_name"),
                rs.getString("server_name"),
                type,
                renderedCommand,
                renderedDebug,
                EpochSeconds.toMillis(rs.getLong("created_at"))
        );
    }

    @Blocking
    public void insert(long profileId,
                       int playerId,
                       int checkId,
                       PunishmentType type,
                       int commandId,
                       @Nullable String commandArgs,
                       @Nullable Integer debugId,
                       @Nullable String debugArgs,
                       long createdAtEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.INSERT_PUNISHMENT)) {
            stmt.setLong(1, profileId);
            stmt.setInt(2, playerId);
            stmt.setInt(3, checkId);
            stmt.setInt(4, type.ordinal());
            stmt.setInt(5, commandId);
            if (commandArgs == null) stmt.setNull(6, Types.VARCHAR);
            else stmt.setString(6, commandArgs);
            if (debugId == null) stmt.setNull(7, Types.INTEGER);
            else stmt.setInt(7, debugId);
            if (debugArgs == null) stmt.setNull(8, Types.VARCHAR);
            else stmt.setString(8, debugArgs);
            stmt.setInt(9, EpochSeconds.fromMillis(createdAtEpochMs));
            stmt.executeUpdate();
        }
    }

    @Blocking
    public List<PunishmentRecord> findByPlayer(UUID uuid, int limit, int offset) throws SQLException {
        return findRows(Sql.SELECT_PUNISHMENTS_BY_UUID, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
        });
    }

    @Blocking
    public int countByPlayer(UUID uuid) throws SQLException {
        return countRows(Sql.COUNT_PUNISHMENTS_BY_UUID, stmt -> stmt.setBytes(1, UuidBytes.toBytes(uuid)));
    }

    @Blocking
    public List<PunishmentRecord> findByPlayerSince(UUID uuid, long sinceEpochMs, int limit, int offset) throws SQLException {
        return findRows(Sql.SELECT_PUNISHMENTS_BY_UUID_SINCE, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, EpochSeconds.fromMillis(sinceEpochMs));
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
        });
    }

    @Blocking
    public int countByPlayerSince(UUID uuid, long sinceEpochMs) throws SQLException {
        return countRows(Sql.COUNT_PUNISHMENTS_BY_UUID_SINCE, stmt -> {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, EpochSeconds.fromMillis(sinceEpochMs));
        });
    }

    @Blocking
    public long deleteByPlayer(UUID uuid, int chunkSize) throws SQLException {
        long total = 0;
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.DELETE_PUNISHMENTS_BY_UUID)) {
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

    private List<PunishmentRecord> findRows(String sql, StatementBinder binder) throws SQLException {
        List<PunishmentRecord> out = new ArrayList<>();
        PunishmentType[] types = PunishmentType.values();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(sql)) {
            binder.bind(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) out.add(readRow(rs, types));
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
