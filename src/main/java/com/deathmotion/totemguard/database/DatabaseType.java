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

import com.alessiodp.libby.Library;
import com.deathmotion.totemguard.database.adapters.H2DatabaseTypeAdapter;
import com.deathmotion.totemguard.database.adapters.MariaDbDatabaseTypeAdapter;
import com.deathmotion.totemguard.database.adapters.MysqlDatabaseTypeAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

// Thanks, Jones (Sonar: https://github.com/jonesdevelopment/sonar/blob/main/api/src/main/java/xyz/jonesdev/sonar/api/config/SonarConfiguration.java) for this enumeration

@Getter
@RequiredArgsConstructor
public enum DatabaseType {
    MYSQL("MySQL", "jdbc:mysql://%s:%d/%s", new MysqlDatabaseTypeAdapter(),
            Library.builder()
                    .groupId("com{}mysql")
                    .artifactId("mysql-connector-j")
                    .version("9.0.0")
                    .relocate("com{}mysql", "com{}deathmotion{}totemguard{}libs{}mysql")
                    .build()),
    MARIADB("MariaDB", "jdbc:mariadb://%s:%d/%s", new MariaDbDatabaseTypeAdapter(),
            Library.builder()
                    .groupId("org{}mariadb{}jdbc")
                    .artifactId("mariadb-java-client")
                    .version("3.4.1")
                    .relocate("org{}mariadb", "com{}deathmotion{}totemguard{}libs{}mariadb")
                    .build()),
    H2("H2", "jdbc:h2:file:%s", new H2DatabaseTypeAdapter(),
            Library.builder()
                    .groupId("com{}h2database")
                    .artifactId("h2")
                    .version("2.2.220")
                    .relocate("org{}h2", "com{}deathmotion{}totemguard{}libs{}h2")
                    .build());

    private final String displayName;
    private final String connectionString;
    private final com.j256.ormlite.db.DatabaseType databaseType;
    private final Library databaseDriver;

    @Setter
    private boolean loaded;

    public static DatabaseType fromString(String type) {
        for (DatabaseType databaseType : DatabaseType.values()) {
            if (databaseType.displayName.equalsIgnoreCase(type)) {
                return databaseType;
            }
        }
        throw new IllegalArgumentException("Invalid database type: " + type);
    }
}
