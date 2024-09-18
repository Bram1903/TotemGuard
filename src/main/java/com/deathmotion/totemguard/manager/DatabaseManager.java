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
import com.deathmotion.totemguard.config.Settings;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import io.ebean.migration.MigrationConfig;
import lombok.Getter;

@Getter
public class DatabaseManager {
    private final Database database;

    public DatabaseManager(TotemGuard plugin) {
        Settings.Database settings = plugin.getConfigManager().getSettings().getDatabase();
        DataSourceConfig dataSourceConfig = createDataSourceConfig(settings, plugin);
        DatabaseConfig databaseConfig = createDatabaseConfig(dataSourceConfig);

        this.database = initializeDatabase(databaseConfig, plugin);
    }

    private DataSourceConfig createDataSourceConfig(Settings.Database settings, TotemGuard plugin) {
        DataSourceConfig dataSourceConfig = new DataSourceConfig();
        switch (settings.getType()) {
            case SQLITE:
                configureSQLite(dataSourceConfig, plugin);
                break;
            case MYSQL:
                configureMySQL(dataSourceConfig, settings);
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + settings.getType());
        }
        return dataSourceConfig;
    }

    private void configureSQLite(DataSourceConfig config, TotemGuard plugin) {
        config.setUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/data.db");
        config.setUsername("root");
        config.setPassword("root");
    }

    private void configureMySQL(DataSourceConfig config, Settings.Database settings) {
        config.setUrl("jdbc:mysql://" + settings.getHost() + ":" + settings.getPort() + "/" + settings.getName());
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
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