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

package com.deathmotion.totemguard.bootstrap;

import lombok.Getter;

@Getter
public enum Library {
    CONFIGLIB("com.github.Exlll.ConfigLib", "configlib-yaml", "v4.6.1"),
    LETTUCE("io.lettuce", "lettuce-core", "6.5.1.RELEASE"),
    DISCORD_WEBHOOK("club.minnced", "discord-webhooks", "0.8.0"),
    EXPIRING_MAP("net.jodah", "expiringmap", "0.5.11"),

    // Database
    ORMLITE("com.j256.ormlite", "ormlite-jdbc", "6.1"),
    HIKARI_CP("com.zaxxer", "HikariCP", "6.3.0"),
    MYSQL("com.mysql", "mysql-connector-j", "9.0.0"),
    MARIADB("org.mariadb.jdbc", "mariadb-java-client", "3.4.1"),
    H2("com.h2database", "h2", "2.2.220");

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