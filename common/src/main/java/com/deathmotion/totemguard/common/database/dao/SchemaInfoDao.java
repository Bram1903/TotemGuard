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
import java.util.HashMap;
import java.util.Map;

public final class SchemaInfoDao {

    private final DatabaseConnectionManager connection;

    public SchemaInfoDao(DatabaseConnectionManager connection) {
        this.connection = connection;
    }

    @Blocking
    public Map<String, TableSize> tableSizes() throws SQLException {
        Map<String, TableSize> out = new HashMap<>();
        try (Connection c = connection.borrow();
             PreparedStatement stmt = c.prepareStatement(Sql.SELECT_TG_TABLE_SIZES);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("table_name");
                long rows = rs.getLong("table_rows");
                long avgRowLength = rs.getLong("avg_row_length");
                out.put(name, new TableSize(name, rows, avgRowLength));
            }
        }
        return out;
    }

    public record TableSize(String name, long rows, long avgRowLength) {
    }
}
