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

import com.deathmotion.totemguard.common.database.Sql;
import org.jetbrains.annotations.Blocking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the bundled schema DDL.
 *
 * <p>DDL statements use {@code CREATE TABLE IF NOT EXISTS}, so this runs
 * safely on every startup. When schema evolution is needed we'll stamp
 * {@code tg_schema_version} and apply migrations conditionally.</p>
 */
public final class SchemaInitializer {

    @Blocking
    public void apply(Connection connection) throws SQLException {
        List<String> statements = loadStatements();
        boolean previousAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(true);
            try (Statement stmt = connection.createStatement()) {
                for (String sql : statements) {
                    stmt.execute(sql);
                }
            }
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private List<String> loadStatements() {
        String raw;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(Sql.SCHEMA_RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Schema resource not found: " + Sql.SCHEMA_RESOURCE_PATH);
            }
            StringBuilder buffer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int comment = line.indexOf("--");
                    if (comment >= 0) line = line.substring(0, comment);
                    buffer.append(line).append('\n');
                }
            }
            raw = buffer.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load schema " + Sql.SCHEMA_RESOURCE_PATH, ex);
        }

        List<String> statements = new ArrayList<>();
        for (String stmt : raw.split(";")) {
            String trimmed = stmt.trim();
            if (!trimmed.isEmpty()) statements.add(trimmed);
        }
        return statements;
    }
}
