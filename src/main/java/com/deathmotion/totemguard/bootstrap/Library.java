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

package com.deathmotion.totemguard.bootstrap;

import lombok.Getter;

@Getter
public enum Library {
    LETTUCE_CORE("io.lettuce", "lettuce-core", "6.5.1.RELEASE"),
    EBEAN_CORE("io.ebean", "ebean-core", "15.8.0"),
    EBEAN_DATASOURCE("io.ebean", "ebean-datasource", "9.0"),
    EBEAN_MIGRATION("io.ebean", "ebean-migration", "14.2.0"),
    EBEAN_H2("io.ebean", "ebean-platform-h2", "15.8.0"),
    EBEAN_SQLITE("io.ebean", "ebean-platform-sqlite", "15.8.0"),
    EBEAN_MYSQL("io.ebean", "ebean-platform-mysql", "15.8.0"),
    EBEAN_MARIADB("io.ebean", "ebean-platform-mariadb", "15.8.0"),
    EBEAN_POSTGRES("io.ebean", "ebean-platform-postgres", "15.8.0"),

    // SQL Drivers
    H2("com.h2database", "h2", "2.3.232"),
    MYSQL("mysql", "mysql-connector-java", "8.0.30"),
    POSTGRES("org.postgresql", "postgresql", "42.7.4"),
    MARIADB("org.mariadb.jdbc", "mariadb-java-client", "3.5.1"),
    SQLITE("org.xerial", "sqlite-jdbc", "3.47.1.0"),

    // Other Libraries
    CONFIGLIB("de.exlll", "configlib-paper", "4.5.0"),
    DISCORD_WEBHOOKS("club.minnced", "discord-webhooks", "0.8.0"),
    EXPIRING_MAP("net.jodah", "expiringmap", "0.5.11");


    private final String group;
    private final String name;
    private final String version;

    Library(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public String getMavenDependency() {
        return String.format("%s:%s:%s", group, name, version);
    }
}