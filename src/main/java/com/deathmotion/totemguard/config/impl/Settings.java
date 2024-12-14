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

package com.deathmotion.totemguard.config.impl;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@Configuration
@Getter
public final class Settings {

    @Comment("Prefix: Sets the command prefix for the plugin.")
    private String Prefix = "&6&lTG &8» ";

    @Comment("\nAPI: Enables the TotemGuard API.")
    private boolean Api = true;

    @Comment("\nServer Name: The name of the server. (Used for alerts, webhooks, API, etc.)")
    private String Server = "Default";

    @Comment("\nClient Brand Notifier: Notifies players with the alert permission, what client brand a player is using.")
    private boolean AlertBrand = false;

    @Comment({
            "",
            "Supported Placeholders for alerts and punishments:",
            "%prefix% - Prefix of the Plugin",
            "%uuid% - UUID of the Player",
            "%player% - Name of the Player",
            "%check% - Name of the Check",
            "%description% - Description of the Check",
            "%ping% - Player's Ping",
            "%tps% - Server's TPS",
            "%server% - Server Name",
            "%punishable% - If the check is punishable",
            "%violations% - Amount of Violations",
            "%max_violations% - Maximum Violations",
            "",
            "Alert Format: The format of the alert message."})
    private String AlertFormat = "%prefix%&e%player%&7 failed &6%check%&f &7VL[&6%violations%/%max_violations%&7]";

    @Comment("\nAlerts Enabled: Message when enabling alerts.")
    private String AlertsEnabled = "%prefix%&aAlerts enabled";

    @Comment("\nAlerts Disabled: Message when disabling alerts.")
    private String AlertsDisabled = "%prefix%&cAlerts disabled";

    @Comment("When enabled, players with the bypass permission will not be flagged.")
    private boolean Bypass = false;

    @Comment("\nProxy Alert Settings:")
    private ProxyAlerts ProxyAlerts = new ProxyAlerts();

    @Comment("\nColor Scheme Settings:")
    private ColorScheme ColorScheme = new ColorScheme();

    @Comment("\nThe time in minutes at which the plugin should reset the violations.")
    private int ResetViolationsInterval = 30;

    @Comment("\nUpdate Checker Settings:")
    private UpdateChecker UpdateChecker = new UpdateChecker();

    @Comment("\nDetermines when the plugin should stop for checking a player.")
    private Determine Determine = new Determine();

    @Comment("\nDatabase settings:")
    private Database Database = new Database();

    @Comment("\nDebug: Enables debug mode (Advanced Users Only).")
    private boolean Debug = false;

    @Configuration
    @Getter
    public static class ProxyAlerts {
        @Comment({
                "Proxy messaging method",
                "How should be send and receive messages from sibling servers?",
                "Options:",
                " - plugin-messaging (Will use plugin messaging through player connections.)",
                " - redis (Requires further configuration in the 'redis' section below.)"
        })
        private String Method = "plugin-messaging";

        @Comment("\nChannel: The channel to send and receive alerts.")
        private String Channel = "totemguard";

        @Comment("\nWhen enabled, the plugin will send alerts to other servers connected to the proxy.")
        private boolean Send = true;

        @Comment("\nWhen enabled, the plugin will receive alerts from other servers connected to the proxy.")
        private boolean Receive = true;

        @Comment("\nRedis Configuration")
        private RedisConfiguration Redis = new RedisConfiguration();

        @Configuration
        @Getter
        public static class RedisConfiguration {
            private String Host = "localhost";
            private int Port = 6379;
            private String Username = "default";
            private String Password = "yourPassword";
        }
    }

    @Configuration
    @Getter
    public static class ColorScheme {
        @Comment("Primary Color: The primary color of the plugin.")
        private String PrimaryColor = "&6";

        @Comment("\nSecondary Color: The secondary color of the plugin.")
        private String SecondaryColor = "&7";
    }

    @Configuration
    @Getter
    public static class Database {
        @Comment("Database Type: The type of database to use. (SQLite, MYSQL)")
        private String Type = "SQLITE";

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

    @Configuration
    @Getter
    public static class UpdateChecker {
        @Comment("Enable and/or disable the update checker.")
        private boolean Enabled = true;

        @Comment("\nPrint to Console: Prints the update message to the console.")
        private boolean PrintToConsole = true;

        @Comment("\nNotify In-Game: Notifies players with the permission in-game.")
        private boolean NotifyInGame = true;
    }

    @Configuration
    @Getter
    public static class Determine {
        @Comment("Minimum TPS.")
        private double MinTps = 15.0;

        @Comment("\nMaximum Ping.")
        private int MaxPing = 400;
    }
}