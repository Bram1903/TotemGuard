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

import com.deathmotion.totemguard.api3.punishment.PunishmentType;
import com.deathmotion.totemguard.common.database.DatabaseConnectionManager;
import com.deathmotion.totemguard.common.database.Sql;
import com.deathmotion.totemguard.common.database.model.PunishmentRecord;
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

    private static String truncate(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, max);
    }

    @Blocking
    public void insert(
            @Nullable Long profileId,
            int playerId,
            int serverId,
            int checkId,
            PunishmentType type,
            String expandedCommand,
            @Nullable String debug,
            long createdAt
    ) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.INSERT_PUNISHMENT)) {
            if (profileId == null) stmt.setNull(1, Types.BIGINT);
            else stmt.setLong(1, profileId);
            stmt.setInt(2, playerId);
            stmt.setInt(3, serverId);
            stmt.setInt(4, checkId);
            stmt.setInt(5, type.ordinal());
            stmt.setString(6, truncate(expandedCommand, 256));
            if (debug == null) stmt.setNull(7, Types.VARCHAR);
            else stmt.setString(7, truncate(debug, 128));
            stmt.setLong(8, createdAt);
            stmt.executeUpdate();
        }
    }

    @Blocking
    public List<PunishmentRecord> findByPlayer(UUID uuid, int limit, int offset) throws SQLException {
        List<PunishmentRecord> out = new ArrayList<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_PUNISHMENTS_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                PunishmentType[] types = PunishmentType.values();
                while (rs.next()) {
                    int ordinal = rs.getInt("type");
                    PunishmentType type = (ordinal >= 0 && ordinal < types.length)
                            ? types[ordinal]
                            : PunishmentType.GENERIC;
                    out.add(new PunishmentRecord(
                            rs.getLong("id"),
                            rs.getString("check_name"),
                            rs.getString("server_name"),
                            type,
                            rs.getString("command"),
                            rs.getString("debug"),
                            rs.getLong("created_at")
                    ));
                }
            }
        }
        return out;
    }

    @Blocking
    public int countByPlayer(UUID uuid) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_PUNISHMENTS_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Blocking
    public long deleteByPlayer(UUID uuid) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.DELETE_PUNISHMENTS_BY_UUID)) {
            stmt.setBytes(1, UuidBytes.toBytes(uuid));
            return stmt.executeUpdate();
        }
    }

    @Blocking
    public int countAll() throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_PUNISHMENTS_TOTAL);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    @Blocking
    public int countSince(long sinceEpochMs) throws SQLException {
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.COUNT_PUNISHMENTS_SINCE)) {
            stmt.setLong(1, sinceEpochMs);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
