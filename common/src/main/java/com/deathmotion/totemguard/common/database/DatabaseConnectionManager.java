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
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Owns the HikariCP pool connecting to an external MySQL-compatible server
 * (MySQL 8+, MariaDB, etc.).
 *
 * <p>The JDBC driver itself is not bundled in the plugin jar — it's declared
 * as a runtime library in the platform descriptors ({@code plugin.yml} for
 * Paper, {@code velocity-plugin.json} for Velocity), so the server downloads
 * and loads it into the plugin classloader on startup.</p>
 */
public final class DatabaseConnectionManager {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private volatile @Nullable HikariDataSource dataSource;

    public synchronized void start(DatabaseOptions options) {
        if (!options.isEnabled()) return;
        loadDriver();

        HikariConfig config = new HikariConfig();
        config.setPoolName("TotemGuard-DB");
        config.setMaximumPoolSize(DatabaseOptions.POOL_MAX_SIZE);
        config.setMinimumIdle(DatabaseOptions.POOL_MIN_IDLE);
        config.setConnectionTimeout(DatabaseOptions.POOL_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(DatabaseOptions.POOL_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DatabaseOptions.POOL_MAX_LIFETIME_MS);
        // A negative fail-timeout lets the pool come up even when the DB is
        // temporarily unreachable — Hikari will lazily connect on first borrow
        // and transparently recreate connections once the server is back.
        config.setInitializationFailTimeout(-1);
        config.setDriverClassName(DRIVER_CLASS);

        // MySQL Connector/J speaks to both MySQL and MariaDB over the MySQL
        // wire protocol, so the same URL + driver cover both backends.
        StringBuilder url = new StringBuilder("jdbc:mysql://")
                .append(options.getHost())
                .append(':').append(options.getPort())
                .append('/').append(options.getDatabase());
        String params = options.getParameters();
        if (params != null && !params.isBlank()) {
            url.append('?').append(params);
        }
        config.setJdbcUrl(url.toString());
        config.setUsername(options.getUsername());
        config.setPassword(options.getPassword());
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
            // Driver is declared as a runtime library in plugin.yml /
            // velocity-plugin.json — a missing class here means the platform
            // failed to resolve that library (no network, Maven Central down,
            // etc.). Fail loudly so the admin can fix it.
            throw new IllegalStateException(
                    "MySQL JDBC driver (" + DRIVER_CLASS + ") is not on the classpath. " +
                            "Make sure the platform's library loader has access to Maven Central, " +
                            "or install mysql-connector-j into the server's library folder.",
                    ex);
        }
    }
}
