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

package com.deathmotion.totemguard.config;

import com.deathmotion.totemguard.models.ICheckSettings;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

import java.util.List;

@Configuration
@Getter
public final class Settings {

    @Comment("Prefix: Sets the command prefix for the plugin.")
    private String Prefix = "&6&lTG &8» ";

    @Comment("\nDebug: Enables debug mode (Advanced Users Only).")
    private boolean Debug = false;

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

    @Comment("\nWebhook settings:")
    private Webhook Webhook = new Webhook();

    @Comment("\nChecks")
    private Checks Checks = new Checks();

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
    public static class Webhook {
        @Comment("Webhook Alert Settings")
        private AlertSettings Alert = new AlertSettings();

        @Comment("\nWebhook Punishment Settings")
        private PunishmentSettings Punishment = new PunishmentSettings();

        @Configuration
        @Getter
        public abstract static class WebhookSettings {
            @Comment("Enable and/or disable the webhook implementation.")
            private boolean Enabled = false;

            @Comment("\nWebhook URL: The URL of the webhook to send notifications to.")
            private String Url = "https://discord.com/api/webhooks/your_webhook_url";

            @Comment("\nClient Name: Name of the client.")
            private String Name = "TotemGuard";

            @Comment("\nWebhook Embed color: Color of the webhook embed (in hex).")
            private String Color;

            @Comment("\nWebhook Title: Brief description about what the webhook is about. (Like Alert, Punishment, etc.)")
            private String Title;

            @Comment("\nWebhook Profile Image: Sets the image of the embed's profile.")
            private String ProfileImage = "https://i.imgur.com/hqaGO5H.png";

            @Comment("\nWebhook Timestamp: Displays the time that this embed was sent at.")
            private boolean Timestamp = true;

            public WebhookSettings(String title, String color) {
                this.Title = title;
                this.Color = color;
            }
        }

        @Configuration
        @Getter
        public static class AlertSettings extends WebhookSettings {
            public AlertSettings() {
                super("TotemGuard Alert", "#d9b61a");
            }
        }

        @Configuration
        @Getter
        public static class PunishmentSettings extends WebhookSettings {
            public PunishmentSettings() {
                super("TotemGuard Punishment", "#d60010");
            }
        }
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

    @Configuration
    @Getter
    public static class Checks {
        @Comment("When enabled, players with the bypass permission will not be flagged.")
        private boolean Bypass = false;

        @Comment("\nThis command placeholder can be used by using %default_punishment% as a punishment command.")
        private String DefaultPunishment = "ban %player% 1d [TotemGuard] Unfair Advantage";

        @Comment({"", "AutoTotemA Settings"})
        private AutoTotemA AutoTotemA = new AutoTotemA();

        @Comment("\nAutoTotemB Settings")
        private AutoTotemB AutoTotemB = new AutoTotemB();

        @Comment("\nAutoTotemC Settings")
        private AutoTotemC AutoTotemC = new AutoTotemC();

        @Comment("\nAutoTotemD Settings")
        private AutoTotemD AutoTotemD = new AutoTotemD();

        @Comment("\nAutoTotemE Settings")
        private AutoTotemE AutoTotemE = new AutoTotemE();

        @Comment("\nAutoTotemF Settings")
        private AutoTotemF AutoTotemF = new AutoTotemF();

        @Comment("\nBadPacketA Settings")
        private BadPacketsA BadPacketsA = new BadPacketsA();

        @Comment("\nBadPacketB Settings")
        private BadPacketsB BadPacketB = new BadPacketsB();

        @Comment("\nBadPacketC Settings")
        private BadPacketsC BadPacketC = new BadPacketsC();

        @Comment("\nManualTotemA Settings")
        private ManualTotemA ManualTotemA = new ManualTotemA();

        @Comment("\nManualBan Settings")
        private ManualBan ManualBan = new ManualBan();

        @Configuration
        @Getter
        public abstract static class CheckSettings implements ICheckSettings {
            private boolean Enabled = true;
            private boolean Punishable;
            private int PunishmentDelayInSeconds = 0;
            private int MaxViolations;
            private List<String> PunishmentCommands = List.of(
                    "%default_punishment%"
            );

            public CheckSettings(boolean punishable, int punishmentDelay, int maxViolations) {
                this.Punishable = punishable;
                this.PunishmentDelayInSeconds = punishmentDelay;
                this.MaxViolations = maxViolations;
            }

            public CheckSettings(boolean punishable, int maxViolations) {
                this.Punishable = punishable;
                this.MaxViolations = maxViolations;
            }
        }

        @Configuration
        @Getter
        public static class AutoTotemA extends CheckSettings {
            @Comment("\nNormal Check Time: Sets the interval (in ms) for normal checks.")
            private int NormalCheckTimeMs = 1500;

            @Comment("\nClick Time Difference: The value (in ms) which anything below will trigger the flag.")
            private int ClickTimeDifference = 75;

            public AutoTotemA() {
                super(true, 2);
            }
        }

        @Configuration
        @Getter
        public static class AutoTotemB extends CheckSettings {
            @Comment("\nStandard Deviation Threshold: The threshold for the standard deviation.")
            private double StandardDeviationThreshold = 30.0;

            @Comment("\nMean Threshold: The threshold for the mean.")
            private double MeanThreshold = 500.0;

            @Comment("\nConsecutive Low SD Count: The amount of consecutive low standard deviations before flagging.")
            private int ConsecutiveLowSDCount = 3;

            public AutoTotemB() {
                super(true, 6);
            }
        }

        @Configuration
        @Getter
        public static class AutoTotemC extends CheckSettings {
            @Comment("\nConsistent SD Range: The range for the standard average deviation.")
            private double ConsistentSDRange = 1.0;

            @Comment("\nConsecutive Violations: The amount of consecutive violations before flagging.")
            private int ConsecutiveViolations = 3;

            public AutoTotemC() {
                super(true, 3);
            }
        }

        @Configuration
        @Getter
        public static class AutoTotemD extends CheckSettings {
            @Comment("\nTotal Sequence: The total sequence timing under which the player will be flagged.")
            private int TotalSequence = 160;

            @Comment("\nTime average Difference between packets: The time difference between packets.")
            private int BaseTimeDifference = 50;

            @Comment("\nTime Tolerance: The tolerance for the time difference.")
            private int Tolerance = 5;

            public AutoTotemD() {
                super(true, 2);
            }
        }

        @Configuration
        @Getter
        public static class AutoTotemE extends CheckSettings {
            @Comment("\nStandard Deviation Threshold: The threshold for the standard deviation.")
            private double StandardDeviationThreshold = 10.0;

            @Comment("\nAverage Standard Deviation Threshold: The threshold for the average standard deviation.")
            private double AverageStDeviationThreshold = 10.0;

            public AutoTotemE() {
                super(true, 4);
            }
        }

        @Configuration
        @Getter
        public static class AutoTotemF extends CheckSettings {
            @Comment("\nTime Difference: The time difference between closing the inventory and the last click.")
            private int TimeDifference = 1500;

            public AutoTotemF() {
                super(false, 6);
            }
        }

        @Configuration
        @Getter
        public static class BadPacketsA extends CheckSettings {
            public BadPacketsA() {
                super(true, 20, 1);
            }
        }


        @Configuration
        @Getter
        public static class BadPacketsB extends CheckSettings {
            @Comment("\nBanned Client Brands: The list of client brands to flag.")
            private List<String> BannedClientBrands = List.of(
                    "autototem"
            );

            public BadPacketsB() {
                super(true, 20, 1);
            }
        }

        @Configuration
        @Getter
        public static class BadPacketsC extends CheckSettings {
            public BadPacketsC() {
                super(true, 20, 3);
            }
        }

        @Configuration
        @Getter
        public static class ManualTotemA extends CheckSettings {
            @Comment("\nCheck Time: Amount of time the check command waits for a retotem. (in ms)")
            private int CheckTime = 400;

            public ManualTotemA() {
                super(false, 4);
            }
        }

        @Configuration
        @Getter
        public static class ManualBan {
            private List<String> PunishmentCommands = List.of(
                    "%default_punishment%"
            );
        }
    }
}