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

package com.deathmotion.totemguard.config;

import com.deathmotion.totemguard.database.DatabaseType;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Settings {
    @Comment("API: Whether the API should be enabled.")
    private boolean api = true;

    @Comment("\nServer Name: The name of the server. (Used for alerts, webhooks, API, etc.)")
    private String server = "Default";

    @Comment("\nBypass: Whether players with the permission 'totemguard.bypass' can bypass checks.")
    private boolean bypass = false;

    @Comment("\nConsole Alerts: Whether the console should receive alerts.")
    private boolean consoleAlerts = true;

    @Comment("\nAnnounce client brand: Whether the client brand should be announced upon a player joining.")
    private boolean announceClientBrand = false;

    @Comment("\nRedis Configuration")
    private Redis redis = new Redis();

    @Comment("\nUpdate Checker Settings:")
    private UpdateChecker updateChecker = new UpdateChecker();

    @Comment("\nDatabase Settings:")
    private Database database = new Database();

    @Comment("\nDebug: Enables debug mode (Advanced Users Only).")
    private boolean debug = false;

    @Configuration
    @Getter
    public static class Redis {
        @Comment("Enable and/or disable Redis.")
        private boolean enabled = false;

        @Comment("\nSync Alerts: Whether alerts should be synced across servers.")
        private boolean syncAlerts = true;

        @Comment("\nChannel: The redis channel the packets will be sent over.")
        private String channel = "totemguard";

        private String host = "localhost";
        private int port = 6379;
        private String username = "default";
        private String password = "yourPassword";
    }

    @Configuration
    @Getter
    public static class UpdateChecker {
        @Comment("Enable and/or disable the update checker.")
        private boolean enabled = true;

        @Comment("\nPrint to Console: Prints the update message to the console.")
        private boolean printToConsole = true;

        @Comment("\nNotify In-Game: Notifies players with the permission in-game.")
        private boolean notifyInGame = true;
    }

    @Configuration
    @Getter
    public static class Database {
        @Comment("Database Type: The type of database to use. (h2, mysql, mariadb)")
        private String Type = DatabaseType.H2.getDisplayName();

        @Comment("\nDatabase Host: The host of the database.")
        private String Host = "localhost";

        @Comment("\nDatabase Port: The port of the database.")
        private int Port = 3306;

        @Comment("\nDatabase Name: The name of the database.")
        private String Name = "TotemGuard";

        @Comment("\nDatabase Username: The username of the database.")
        private String Username = "root";

        @Comment("\nDatabase Password: The password of the database.")
        private String Password = "password";
    }
}