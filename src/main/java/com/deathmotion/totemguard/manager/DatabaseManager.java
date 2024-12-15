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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

@Getter
public class DatabaseManager {
    private final Database database;

    public DatabaseManager(TotemGuard plugin) {
        Settings.Database settings = plugin.getConfigManager().getSettings().getDatabase();
        DataSourceConfig dataSourceConfig = configureDataSource(settings, plugin);
        DatabaseConfig databaseConfig = createDatabaseConfig(dataSourceConfig);

        this.database = initializeDatabase(databaseConfig, plugin);
    }

    public void close() {
        database.shutdown(true, true);
    }

    private Database initializeDatabase(DatabaseConfig config, TotemGuard plugin) {
        URLClassLoader customClassLoader = createCustomClassLoader(plugin);

        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();

        try {
            currentThread.setContextClassLoader(customClassLoader);
            return DatabaseFactory.createWithContextClassLoader(config, customClassLoader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database.", e);
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
    }

    private URLClassLoader createCustomClassLoader(TotemGuard plugin) {
        try {
            List<URL> jarUrls = loadLibraryUrls(plugin);
            return new URLClassLoader(jarUrls.toArray(new URL[0]), plugin.getClass().getClassLoader());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create custom class loader.", e);
        }
    }

    private List<URL> loadLibraryUrls(TotemGuard plugin) throws Exception {
        File librariesRoot = new File(plugin.getServer().getWorldContainer(), "libraries");
        List<URL> jarUrls = new ArrayList<>();

        for (String path : getLibraryPaths()) {
            File jarFile = new File(librariesRoot, path);
            if (jarFile.exists()) {
                jarUrls.add(jarFile.toURI().toURL());
            } else {
                throw new FileNotFoundException("Library not found: " + jarFile.getAbsolutePath());
            }
        }

        return jarUrls;
    }

    private List<String> getLibraryPaths() {
        return List.of(
                "io/ebean/ebean-core/15.8.0/ebean-core-15.8.0.jar",
                "io/ebean/ebean-datasource/9.0/ebean-datasource-9.0.jar",
                "io/ebean/ebean-migration/14.2.0/ebean-migration-14.2.0.jar",
                "io/ebean/ebean-platform-h2/15.8.0/ebean-platform-h2-15.8.0.jar",
                "io/ebean/ebean-platform-mysql/15.8.0/ebean-platform-mysql-15.8.0.jar",
                "io/ebean/ebean-platform-postgres/15.8.0/ebean-platform-postgres-15.8.0.jar",
                "io/ebean/ebean-platform-sqlite/15.8.0/ebean-platform-sqlite-15.8.0.jar",
                "io/ebean/ebean-platform-mariadb/15.8.0/ebean-platform-mariadb-15.8.0.jar",
                "com/h2database/h2/2.3.232/h2-2.3.232.jar",
                "org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar",
                "org/mariadb/jdbc/mariadb-java-client/3.5.1/mariadb-java-client-3.5.1.jar",
                "org/xerial/sqlite-jdbc/3.8.9.1/sqlite-jdbc-3.8.9.1.jar",
                "mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar"
        );
    }

    private DataSourceConfig configureDataSource(Settings.Database settings, TotemGuard plugin) {
        DataSourceConfig config = new DataSourceConfig();
        config.setUsername(settings.getUsername());
        config.setPassword(settings.getPassword());
        config.setUrl(buildJdbcUrl(settings, plugin));
        return config;
    }

    private String buildJdbcUrl(Settings.Database settings, TotemGuard plugin) {
        return switch (settings.getType().toLowerCase()) {
            case "sqlite" -> "jdbc:sqlite:" + new File(plugin.getDataFolder(), "data.db").getAbsolutePath();
            case "mysql" -> buildStandardJdbcUrl("mysql", settings);
            case "postgresql" -> buildStandardJdbcUrl("postgresql", settings);
            case "mariadb" -> buildStandardJdbcUrl("mariadb", settings);
            case "h2" -> "jdbc:h2:file:" + new File(plugin.getDataFolder(), "data").getAbsolutePath();
            default -> throw new IllegalArgumentException("Unsupported database type: " + settings.getType());
        };
    }

    private String buildStandardJdbcUrl(String dbType, Settings.Database settings) {
        return String.format("jdbc:%s://%s:%d/%s", dbType, settings.getHost(), settings.getPort(), settings.getName());
    }

    private DatabaseConfig createDatabaseConfig(DataSourceConfig dataSourceConfig) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSourceConfig(dataSourceConfig);
        config.setRunMigration(true);
        return config;
    }
}