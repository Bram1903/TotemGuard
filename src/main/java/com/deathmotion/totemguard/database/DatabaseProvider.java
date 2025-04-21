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
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.jdbc.db.H2DatabaseType;
import com.j256.ormlite.jdbc.db.MariaDbDatabaseType;
import com.j256.ormlite.jdbc.db.MysqlDatabaseType;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
public class DatabaseProvider {

    private final TotemGuard plugin;

    private final List<String> loadedDatabaseDrivers = new ArrayList<>();

    @Getter
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
        Settings.Database databaseSettings = plugin.getConfigManager().getSettings().getDatabase();
        setConnectionSource(databaseSettings);

        try {
            Logger.setGlobalLogLevel(Level.WARNING);
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

        playerRepository = new PlayerRepository(this);
        alertRepository = new AlertRepository(this);
        punishmentRepository = new PunishmentRepository(this);
        genericService = new DatabaseService(this);

        plugin.getLogger().info("Database repository initialized");
    }

    public void reload() {
        close();
        init();
    }

    public void close() {
        if (connectionSource != null) {
            try {
                connectionSource.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            connectionSource = null;
        }

        if (playerRepository != null) playerRepository = null;
        if (alertRepository != null) alertRepository = null;
        if (punishmentRepository != null) punishmentRepository = null;
    }

    private void setConnectionSource(Settings.Database databaseSettings) {
        String jdbcURL;
        BaseDatabaseType databaseType = switch (databaseSettings.getType().toLowerCase(Locale.ROOT)) {
            case "h2" -> {
                jdbcURL = String.format("jdbc:h2:file:%s", plugin.getDataFolder().getAbsolutePath() + "/db/data");
                yield new H2DatabaseType();
            }
            case "mysql" -> {
                jdbcURL = String.format("jdbc:mysql://%s:%d/%s", databaseSettings.getHost(), databaseSettings.getPort(), databaseSettings.getName());
                yield new MysqlDatabaseType();
            }
            case "mariadb" -> {
                jdbcURL = String.format("jdbc:mariadb://%s:%d/%s", databaseSettings.getHost(), databaseSettings.getPort(), databaseSettings.getName());
                yield new MariaDbDatabaseType();
            }
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseSettings.getType());
        };

        try {
            connectionSource = new JdbcConnectionSource(jdbcURL, databaseSettings.getUsername(), databaseSettings.getPassword(), databaseType);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
