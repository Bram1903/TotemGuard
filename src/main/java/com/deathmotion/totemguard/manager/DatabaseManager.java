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
import io.ebean.Transaction;
import io.ebean.config.DatabaseConfig;
import io.ebean.datasource.DataSourceConfig;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Getter
public class DatabaseManager {

    private final Database database;

    public DatabaseManager(TotemGuard plugin) {
        Settings.Database settings = plugin.getConfigManager().getSettings().getDatabase();
        DataSourceConfig dataSourceConfig = createDataSourceConfig(settings, plugin);
        DatabaseConfig databaseConfig = createDatabaseConfig(dataSourceConfig);

        this.database = initializeDatabase(databaseConfig, plugin);
    }

    public void close() {
        database.shutdown(true, true);
    }

    private Database initializeDatabase(DatabaseConfig config, TotemGuard plugin) {
        try (URLClassLoader customClassLoader = createCustomClassLoader(plugin)) {
            return executeWithCustomClassLoader(customClassLoader, () -> DatabaseFactory.createWithContextClassLoader(config, customClassLoader));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize the database.", e);
        }
    }

    private URLClassLoader createCustomClassLoader(TotemGuard plugin) {
        try {
            List<URL> libraryUrls = loadLibraryUrls(plugin);
            return new URLClassLoader(libraryUrls.toArray(new URL[0]), plugin.getClass().getClassLoader());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create custom class loader.", e);
        }
    }

    private List<URL> loadLibraryUrls(TotemGuard plugin) throws Exception {
        File librariesRoot = new File(plugin.getServer().getWorldContainer(), "libraries");
        List<URL> jarUrls = new ArrayList<>();

        for (String path : getLibraryPaths()) {
            File jarFile = new File(librariesRoot, path);
            if (!jarFile.exists()) {
                throw new FileNotFoundException("Library not found: " + jarFile.getAbsolutePath());
            }
            jarUrls.add(jarFile.toURI().toURL());
        }

        return jarUrls;
    }

    private DataSourceConfig createDataSourceConfig(Settings.Database settings, TotemGuard plugin) {
        DataSourceConfig config = new DataSourceConfig();
        configureAuthentication(settings, config);
        config.setUrl(createJdbcUrl(settings, plugin));

        String driverClass = getDriverClass(settings.getType().toLowerCase());
        loadDriver(driverClass);

        return config;
    }

    private void configureAuthentication(Settings.Database settings, DataSourceConfig config) {
        switch (settings.getType().toLowerCase()) {
            case "mysql", "mariadb", "postgresql" -> {
                config.setUsername(settings.getUsername());
                config.setPassword(settings.getPassword());
            }
            case "sqlite", "h2" -> {
                config.setUsername("root");
                config.setPassword("");
                config.setIsolationLevel(Transaction.SERIALIZABLE);
            }
        }
    }

    private String createJdbcUrl(Settings.Database settings, TotemGuard plugin) {
        return switch (settings.getType().toLowerCase()) {
            case "sqlite", "h2" -> {
                File dbFile = new File(plugin.getDataFolder(), settings.getType().equals("sqlite") ? "db/data.db" : "db/data");
                ensureDirectoryExists(dbFile.getParentFile());
                yield settings.getType().equals("sqlite")
                        ? "jdbc:sqlite:" + dbFile.getAbsolutePath()
                        : "jdbc:h2:file:" + dbFile.getAbsolutePath();
            }
            case "mysql", "postgresql", "mariadb" -> buildStandardJdbcUrl(settings);
            default -> throw new IllegalArgumentException("Unsupported database type: " + settings.getType());
        };
    }

    private void ensureDirectoryExists(File directory) {
        try {
            Files.createDirectories(directory.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create database directory: " + directory.getAbsolutePath(), e);
        }
    }

    private String buildStandardJdbcUrl(Settings.Database settings) {
        return String.format("jdbc:%s://%s:%d/%s",
                settings.getType().toLowerCase(),
                settings.getHost(),
                settings.getPort(),
                settings.getName());
    }

    private String getDriverClass(String databaseType) {
        return switch (databaseType) {
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "mariadb" -> "org.mariadb.jdbc.Driver";
            case "postgresql" -> "org.postgresql.Driver";
            case "sqlite" -> "org.sqlite.JDBC";
            case "h2" -> "org.h2.Driver";
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        };
    }

    private void loadDriver(String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load driver: " + driverClassName, e);
        }
    }

    private DatabaseConfig createDatabaseConfig(DataSourceConfig dataSourceConfig) {
        DatabaseConfig config = new DatabaseConfig();
        config.setDataSourceConfig(dataSourceConfig);
        config.setRunMigration(true);
        return config;
    }

    private <T> T executeWithCustomClassLoader(ClassLoader classLoader, ClassLoaderTask<T> task) throws Exception {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalClassLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(classLoader);
            return task.execute();
        } finally {
            currentThread.setContextClassLoader(originalClassLoader);
        }
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
                "org/xerial/sqlite-jdbc/3.47.1.0/sqlite-jdbc-3.47.1.0.jar",
                "mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar"
        );
    }

    @FunctionalInterface
    private interface ClassLoaderTask<T> {
        T execute() throws Exception;
    }
}
