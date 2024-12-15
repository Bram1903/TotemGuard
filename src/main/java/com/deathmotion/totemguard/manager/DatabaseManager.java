/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.impl.Settings;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import lombok.Getter;

@Getter
public class DatabaseManager {
    private final Database database;

    public DatabaseManager(TotemGuard plugin) {
        Settings.Database settings = plugin.getConfigManager().getSettings().getDatabase();
        DataSourceConfig dataSourceConfig = configureDataSource(settings, plugin);
        DatabaseConfig databaseConfig = createDatabaseConfig(dataSourceConfig);

        this.database = initializeDatabase(databaseConfig, plugin);
    }

    private DataSourceConfig configureDataSource(Settings.Database settings, TotemGuard plugin) {
        DataSourceConfig config = new DataSourceConfig();
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());

        String url = switch (settings.getType().toLowerCase()) {
            case "sqlite" -> "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/data.db";
            case "mysql" -> buildJdbcUrl("mysql", settings);
            case "postgresql" -> buildJdbcUrl("postgresql", settings);
            case "mariadb" -> buildJdbcUrl("mariadb", settings);
            case "h2" -> "jdbc:h2:file:" + plugin.getDataFolder().getAbsolutePath() + "/data";
            default -> throw new IllegalArgumentException("Unsupported database type: " + settings.getType());
        };

        config.setUrl(url);
        return config;
    }

    private String buildJdbcUrl(String dbType, Settings.Database settings) {
        return "jdbc:" + dbType + "://" + settings.getHost() + ":" + settings.getPort() + "/" + settings.getName();
    }

    private DatabaseConfig createDatabaseConfig(DataSourceConfig dataSourceConfig) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSourceConfig(dataSourceConfig);
        config.setRunMigration(true);
        return config;
    }

    private Database initializeDatabase(DatabaseConfig config, TotemGuard plugin) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader pluginClassLoader = plugin.getClass().getClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            return DatabaseFactory.createWithContextClassLoader(config, pluginClassLoader);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
