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

public final class CatalogDao {

    private final DatabaseConnectionManager connection;

    private final ConcurrentMap<String, Integer> serverIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> checkIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> brandIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> debugTemplateIds = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> punishmentCommandIds = new ConcurrentHashMap<>();

    private volatile int thisServerId = -1;

    public CatalogDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    public void resetCache() {
        serverIds.clear();
        checkIds.clear();
        brandIds.clear();
        debugTemplateIds.clear();
        punishmentCommandIds.clear();
        thisServerId = -1;
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
    public int resolveServerId(String name) throws SQLException {
        return resolve(serverIds, name, Sql.UPSERT_SERVER, Sql.SELECT_SERVER_ID, "tg_servers");
    }

    @Blocking
    public int resolveCheckId(String name) throws SQLException {
        return resolve(checkIds, name, Sql.UPSERT_CHECK, Sql.SELECT_CHECK_ID, "tg_checks");
    }

    @Blocking
    public int resolveBrandId(String name) throws SQLException {
        return resolve(brandIds, name, Sql.UPSERT_BRAND, Sql.SELECT_BRAND_ID, "tg_client_brands");
    }

    @Blocking
    public int resolveDebugTemplateId(String template) throws SQLException {
        return resolve(debugTemplateIds, template,
                Sql.UPSERT_DEBUG_TEMPLATE, Sql.SELECT_DEBUG_TEMPLATE_ID, "tg_debug_messages");
    }

    @Blocking
    public int resolvePunishmentCommandId(String command) throws SQLException {
        return resolve(punishmentCommandIds, command,
                Sql.UPSERT_PUNISHMENT_COMMAND, Sql.SELECT_PUNISHMENT_COMMAND_ID, "tg_punishment_commands");
    }

    private int resolve(ConcurrentMap<String, Integer> cache, String value,
                        String upsertSql, String selectSql, String tableLabel) throws SQLException {
        Integer cached = cache.get(value);
        if (cached != null) return cached;

        synchronized (cache) {
            cached = cache.get(value);
            if (cached != null) return cached;

            try (Connection c = connection.borrow()) {
                try (PreparedStatement upsert = c.prepareStatement(upsertSql)) {
                    upsert.setString(1, value);
                    upsert.executeUpdate();
                }
                try (PreparedStatement select = c.prepareStatement(selectSql)) {
                    select.setString(1, value);
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) {
                            int id = rs.getInt(1);
                            cache.put(value, id);
                            return id;
                        }
                    }
                }
            }
            throw new SQLException("Failed to resolve id in " + tableLabel + " for value of length " + value.length());
        }
    }
}
