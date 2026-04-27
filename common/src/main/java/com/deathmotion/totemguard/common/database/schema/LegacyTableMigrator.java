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

package com.deathmotion.totemguard.common.database.schema;

import com.deathmotion.totemguard.common.TGPlatform;
import org.jetbrains.annotations.Blocking;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renames TotemGuard V2 tables to a {@code _old} suffix so they don't shadow the V3
 * schema. V3 uses {@code tg_*} table names so there is no logical conflict; this rename
 * is purely to flag the legacy data as archived.
 * <p>
 * Each candidate is fingerprinted against the V2 schema (column names + JDBC type) before
 * being touched. A table whose name matches but whose shape differs (e.g. an unrelated
 * project also called {@code totemguard_player}) is left alone with a warning.
 */
public final class LegacyTableMigrator {

    private static final String OLD_SUFFIX = "_old";

    /**
     * Expected column shape per V2 table, keyed by lowercase column name. The integer is
     * the JDBC {@link java.sql.Types} code we expect for that column. We compare on the
     * JDBC type rather than the driver-specific TYPE_NAME so that MariaDB / MySQL /
     * future driver renames don't break the match.
     */
    private static final Map<String, Map<String, Integer>> V2_SCHEMA = buildExpectedSchema();

    private static Map<String, Map<String, Integer>> buildExpectedSchema() {
        Map<String, Map<String, Integer>> m = new LinkedHashMap<>();
        m.put("totemguard_alert", Map.of(
                "id", java.sql.Types.BIGINT,
                "checkname", java.sql.Types.VARCHAR,
                "when_created", java.sql.Types.TIMESTAMP,
                "totemguard_player_uuid", java.sql.Types.VARCHAR
        ));
        m.put("totemguard_player", Map.of(
                "uuid", java.sql.Types.VARCHAR,
                "client_brand", java.sql.Types.VARCHAR,
                "when_created", java.sql.Types.BIGINT
        ));
        m.put("totemguard_punishment", Map.of(
                "id", java.sql.Types.BIGINT,
                "checkname", java.sql.Types.VARCHAR,
                "when_created", java.sql.Types.TIMESTAMP,
                "totemguard_player_uuid", java.sql.Types.VARCHAR
        ));
        return m;
    }

    @Blocking
    public void migrate(Connection connection) throws SQLException {
        DatabaseMetaData md = connection.getMetaData();
        String catalog = connection.getCatalog();
        Logger log = TGPlatform.getInstance().getLogger();

        for (Map.Entry<String, Map<String, Integer>> entry : V2_SCHEMA.entrySet()) {
            String table = entry.getKey();
            Map<String, Integer> expected = entry.getValue();

            if (!tableExists(md, catalog, table)) continue;

            Map<String, Integer> actual = fetchColumns(md, catalog, table);
            String mismatch = describeMismatch(expected, actual);
            if (mismatch != null) {
                log.log(Level.WARNING,
                        "Skipping legacy rename of `" + table + "` — schema does not match TotemGuard V2 ("
                                + mismatch + "). Leaving the table untouched.");
                continue;
            }

            String renamed = pickArchiveName(md, catalog, table);
            try (Statement s = connection.createStatement()) {
                s.execute("RENAME TABLE `" + table + "` TO `" + renamed + "`");
            }
            log.info("Renamed legacy TotemGuard V2 table `" + table + "` to `" + renamed + "`");
        }
    }

    private boolean tableExists(DatabaseMetaData md, String catalog, String name) throws SQLException {
        try (ResultSet rs = md.getTables(catalog, null, name, new String[]{"TABLE"})) {
            while (rs.next()) {
                String found = rs.getString("TABLE_NAME");
                if (found != null && found.equalsIgnoreCase(name)) return true;
            }
        }
        return false;
    }

    private Map<String, Integer> fetchColumns(DatabaseMetaData md, String catalog, String table) throws SQLException {
        Map<String, Integer> columns = new HashMap<>();
        try (ResultSet rs = md.getColumns(catalog, null, table, null)) {
            while (rs.next()) {
                String foundTable = rs.getString("TABLE_NAME");
                if (foundTable == null || !foundTable.equalsIgnoreCase(table)) continue;
                String column = rs.getString("COLUMN_NAME");
                int dataType = rs.getInt("DATA_TYPE");
                if (column != null) {
                    columns.put(column.toLowerCase(Locale.ROOT), dataType);
                }
            }
        }
        return columns;
    }

    /**
     * @return {@code null} if the actual schema is a superset of the expected schema with
     * compatible types, otherwise a short human-readable description of the first
     * discrepancy.
     */
    private String describeMismatch(Map<String, Integer> expected, Map<String, Integer> actual) {
        for (Map.Entry<String, Integer> e : expected.entrySet()) {
            String column = e.getKey();
            Integer wanted = e.getValue();
            Integer found = actual.get(column);
            if (found == null) {
                return "missing column `" + column + "`";
            }
            if (!isTypeCompatible(wanted, found)) {
                return "column `" + column + "` has unexpected JDBC type " + found
                        + " (expected " + wanted + ")";
            }
        }
        return null;
    }

    private boolean isTypeCompatible(int expected, int actual) {
        if (expected == actual) return true;
        // Drivers report DATETIME inconsistently across versions: TIMESTAMP, DATE, or the
        // vendor-specific -1100 (MICROSOFT_DATETIMEOFFSET) on weirder stacks. Accept any
        // date/time family for the timestamp column.
        if (expected == java.sql.Types.TIMESTAMP) {
            return actual == java.sql.Types.TIMESTAMP
                    || actual == java.sql.Types.DATE
                    || actual == java.sql.Types.TIMESTAMP_WITH_TIMEZONE
                    || actual == java.sql.Types.TIME;
        }
        if (expected == java.sql.Types.VARCHAR) {
            return actual == java.sql.Types.VARCHAR
                    || actual == java.sql.Types.CHAR
                    || actual == java.sql.Types.LONGVARCHAR
                    || actual == java.sql.Types.NVARCHAR
                    || actual == java.sql.Types.NCHAR;
        }
        if (expected == java.sql.Types.BIGINT) {
            return actual == java.sql.Types.BIGINT
                    || actual == java.sql.Types.NUMERIC
                    || actual == java.sql.Types.DECIMAL;
        }
        return false;
    }

    private String pickArchiveName(DatabaseMetaData md, String catalog, String table) throws SQLException {
        String base = table + OLD_SUFFIX;
        if (!tableExists(md, catalog, base)) return base;
        long stamp = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            String candidate = i == 0 ? base + "_" + stamp : base + "_" + stamp + "_" + i;
            if (!tableExists(md, catalog, candidate)) return candidate;
        }
        throw new SQLException("Could not pick a unique archive name for legacy table " + table);
    }
}
