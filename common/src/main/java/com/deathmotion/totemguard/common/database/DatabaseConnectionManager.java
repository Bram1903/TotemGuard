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

package com.deathmotion.totemguard.common.database;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.DatabaseOptions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public final class DatabaseConnectionManager {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private volatile @Nullable HikariDataSource dataSource;

    public synchronized void start(DatabaseOptions options) {
        if (!options.enabled()) return;
        loadDriver();

        HikariConfig config = new HikariConfig();
        config.setPoolName("TotemGuard-DB");
        config.setMaximumPoolSize(DatabaseTuning.POOL_MAX_SIZE);
        config.setMinimumIdle(DatabaseTuning.POOL_MIN_IDLE);
        config.setConnectionTimeout(DatabaseTuning.POOL_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(DatabaseTuning.POOL_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DatabaseTuning.POOL_MAX_LIFETIME_MS);
        // Lets the pool boot even when the DB is down; Hikari reconnects lazily.
        config.setInitializationFailTimeout(-1);
        config.setDriverClassName(DRIVER_CLASS);

        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(options.host())
                .append(':').append(options.port())
                .append('/').append(options.database());
        String params = options.parameters();
        if (!params.isBlank()) {
            url.append('?').append(params);
        }
        config.setJdbcUrl(url.toString());
        config.setUsername(options.username());
        config.setPassword(options.password());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
    }

    public synchronized void stop() {
        HikariDataSource current = this.dataSource;
        this.dataSource = null;
        if (current != null) {
            try {
                current.close();
            } catch (Exception ex) {
                TGPlatform.getInstance().getLogger().log(Level.WARNING,
                        "Failed to close database pool cleanly", ex);
            }
        }
    }

    public synchronized void restart(DatabaseOptions options) {
        stop();
        start(options);
    }

    public boolean isConnected() {
        HikariDataSource current = this.dataSource;
        return current != null && current.isRunning();
    }

    public @Nullable DataSource dataSource() {
        return this.dataSource;
    }

    public Connection borrow() throws SQLException {
        HikariDataSource current = this.dataSource;
        if (current == null) throw new SQLException("Database pool is not running");
        return current.getConnection();
    }

    private void loadDriver() {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "MySQL JDBC driver (" + DRIVER_CLASS + ") is not on the classpath. " +
                            "Install mysql-connector-j on the server or via the platform's library loader.",
                    ex);
        }
    }
}
