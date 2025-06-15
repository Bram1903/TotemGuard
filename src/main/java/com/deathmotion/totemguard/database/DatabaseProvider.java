/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.database;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.database.entities.DatabaseAlert;
import com.deathmotion.totemguard.database.entities.DatabasePlayer;
import com.deathmotion.totemguard.database.entities.DatabasePunishment;
import com.deathmotion.totemguard.database.repository.impl.AlertRepository;
import com.deathmotion.totemguard.database.repository.impl.PlayerRepository;
import com.deathmotion.totemguard.database.repository.impl.PunishmentRepository;
import com.j256.ormlite.db.BaseDatabaseType;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.jdbc.db.H2DatabaseType;
import com.j256.ormlite.jdbc.db.MariaDbDatabaseType;
import com.j256.ormlite.jdbc.db.MysqlDatabaseType;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;

@Getter
public final class DatabaseProvider {
    private final TotemGuard plugin;
    private HikariDataSource dataSource;
    private ConnectionSource connectionSource;
    private PlayerRepository playerRepository;
    private AlertRepository alertRepository;
    private PunishmentRepository punishmentRepository;
    private DatabaseService genericService;

    public DatabaseProvider(TotemGuard plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        Settings.Database cfg = plugin.getConfigManager().getSettings().getDatabase();
        this.dataSource = createDataSource(cfg);

        try {
            this.connectionSource = new DataSourceConnectionSource(dataSource, getDatabaseType(cfg.getType()));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize ConnectionSource", e);
        }

        try {
            Logger.setGlobalLogLevel(com.j256.ormlite.logger.Level.WARNING);
            TableUtils.createTableIfNotExists(connectionSource, DatabasePlayer.class);
            TableUtils.createTableIfNotExists(connectionSource, DatabaseAlert.class);
            TableUtils.createTableIfNotExists(connectionSource, DatabasePunishment.class);
        } catch (SQLException e) {
            // The below message is directly copied from Sonar source code

            /*
             * This is caused by a duplicate index;
             * I know this isn't the best method of handling it,
             * but I don't know how else I could address this issue.
             */
        }

        this.playerRepository = new PlayerRepository(this);
        this.alertRepository = new AlertRepository(this);
        this.punishmentRepository = new PunishmentRepository(this);
        this.genericService = new DatabaseService(this);
        plugin.getLogger().info("Database provider initialized");
    }

    public void reload() {
        close();
        init();
    }

    public void close() {
        try {
            if (connectionSource != null) {
                connectionSource.close();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error closing ConnectionSource", e);
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private HikariDataSource createDataSource(Settings.Database cfg) {
        HikariConfig config = new HikariConfig();
        // Register driver explicitly to ensure DriverManager can find it
        String type = cfg.getType().toLowerCase(Locale.ROOT);
        switch (type) {
            case "h2" -> config.setDriverClassName("org.h2.Driver");
            case "mysql" -> config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            case "mariadb" -> config.setDriverClassName("org.mariadb.jdbc.Driver");
            default -> throw new IllegalArgumentException("Unsupported DB type: " + cfg.getType());
        }
        config.setJdbcUrl(getJdbcUrl(cfg));
        config.setUsername(cfg.getUsername());
        config.setPassword(cfg.getPassword());

        // Ensure pool keeps all connections open indefinitely
        config.setMaximumPoolSize(cfg.getConnectionPoolSize());
        config.setMinimumIdle(cfg.getConnectionPoolSize());
        config.setIdleTimeout(0);
        config.setMaxLifetime(0);
        return new HikariDataSource(config);
    }

    private String getJdbcUrl(Settings.Database cfg) {
        return switch (cfg.getType().toLowerCase(Locale.ROOT)) {
            case "h2" -> String.format(
                    "jdbc:h2:file:%s;DB_CLOSE_DELAY=-1",
                    plugin.getDataFolder().getAbsolutePath() + "/db/data"
            );
            case "mysql" -> String.format(
                    // Enable driver auto-reconnect
                    "jdbc:mysql://%s:%d/%s?autoReconnect=true&useSSL=false",
                    cfg.getHost(), cfg.getPort(), cfg.getName()
            );
            case "mariadb" -> String.format(
                    // Enable driver auto-reconnect
                    "jdbc:mariadb://%s:%d/%s?autoReconnect=true&useSSL=false",
                    cfg.getHost(), cfg.getPort(), cfg.getName()
            );
            default -> throw new IllegalArgumentException("Unsupported DB type: " + cfg.getType());
        };
    }

    private BaseDatabaseType getDatabaseType(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "h2" -> new H2DatabaseType();
            case "mysql" -> new MysqlDatabaseType();
            case "mariadb" -> new MariaDbDatabaseType();
            default -> throw new IllegalArgumentException("Unsupported DB type: " + type);
        };
    }
}


