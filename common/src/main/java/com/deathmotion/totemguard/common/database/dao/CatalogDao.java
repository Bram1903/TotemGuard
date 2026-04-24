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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Resolves and caches server/check name → id mappings.
 */
public final class CatalogDao {

    private final DatabaseConnectionManager connection;

    private final ConcurrentMap<String, Integer> serverIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> checkIds = new ConcurrentHashMap<>();

    private volatile int thisServerId = -1;

    public CatalogDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    public void resetCache() {
        serverIds.clear();
        checkIds.clear();
        thisServerId = -1;
    }

    @Blocking
    public synchronized int resolveServerId(String name) throws SQLException {
        Integer cached = serverIds.get(name);
        if (cached != null) return cached;

        try (Connection c = connection.borrow()) {
            int id = upsertAndFetchId(c, "tg_servers", Sql.UPSERT_SERVER, name);
            serverIds.put(name, id);
            return id;
        }
    }

    @Blocking
    public int resolveAndCacheThisServerId(String name) throws SQLException {
        int id = resolveServerId(name);
        thisServerId = id;
        return id;
    }

    public int thisServerIdOrThrow() {
        int id = thisServerId;
        if (id <= 0) throw new IllegalStateException("Server id has not been resolved yet");
        return id;
    }

    @Blocking
    public int resolveCheckId(String name) throws SQLException {
        Integer cached = checkIds.get(name);
        if (cached != null) return cached;

        synchronized (checkIds) {
            cached = checkIds.get(name);
            if (cached != null) return cached;

            try (Connection c = connection.borrow()) {
                int id = upsertAndFetchId(c, "tg_checks", Sql.UPSERT_CHECK, name);
                checkIds.put(name, id);
                return id;
            }
        }
    }

    private int upsertAndFetchId(Connection c, String table, String upsertSql, String name) throws SQLException {
        try (PreparedStatement upsert = c.prepareStatement(upsertSql)) {
            upsert.setString(1, name);
            upsert.executeUpdate();
        }
        try (PreparedStatement select = c.prepareStatement("SELECT id FROM " + table + " WHERE name = ?")) {
            select.setString(1, name);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to resolve id for " + table + "." + name);
    }
}
