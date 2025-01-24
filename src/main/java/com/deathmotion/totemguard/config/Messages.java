/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.config;

import com.deathmotion.totemguard.config.formatter.Formatter;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Messages {
    @Comment("Which text formatter to use (MINIMESSAGE, LEGACY)")
    private Formatter format = Formatter.LEGACY;

    @Comment("\nPrefix: The prefix for all messages.")
    private String prefix = "&6&lTG &8»";

    @Comment("\nAlert Format: The format for all alerts.")
    private AlertFormat alertFormat = new AlertFormat();

    @Comment("")
    private String alertsEnabled = "%prefix% &aAlerts enabled";
    private String alertsDisabled = "%prefix% &cAlerts disabled";
    private String alertBrand = "%prefix% &6%player% &7joined using: &6%client_brand%";

    @Comment("\nCommand Messages")
    private CommandMessages commandMessages = new CommandMessages();

    @Configuration
    @Getter
    public static class AlertFormat {
        @Comment("Alert Format: The format for all alerts.")
        private String alertFormat = "%prefix% &e%player%&7 failed &6%check_name% &7VL[&6%violations%/%max_violations%&7]%dev%";

        @Comment("\nAlert Format Console: The format for all alerts in console.")
        private String alertFormatConsole = "%prefix% &e%player%&7 failed &6%check_name% &7VL[&6%violations%/%max_violations%&7]%dev%";

        @Comment("\nAlert Hover Format: The format for all alerts when hovered over.")
        private String alertHoverMessage = """
                &7TPS: &6%tps%&8 |&7 Client Version: &6%client_version%&8 |&7 Client Brand: &6%client_brand%
                
                &7Player: &6%player%
                &7Ping: &6%ping%ms
                
                &7Check: &6%check_name%
                &7Description: &6%check_description%
                &7Server: &6%server%
                
                %check_details%
                
                &7Click to &6teleport &7to %player%.""";

        @Comment("\nAlert Click Command: The command to run when the alert is clicked.")
        private String alertClickCommand = "/tp %player%";

        @Comment("\nPrefix for experimental checks. Replaces the %dev% placeholder.")
        private String devPrefix = "&d&l *";

        @Comment("\nCheck Details Color: The color for the alert details.")
        private CheckDetailsColor checkDetailsColor = new CheckDetailsColor();

        @Configuration
        @Getter
        public static class CheckDetailsColor {
            @Comment("Main Color: The main color for the check details.")
            private String main = "&7";

            @Comment("\nSecondary Color: The secondary color for the check details.")
            private String secondary = "&6";
        }
    }

    @Configuration
    @Getter
    public static class CommandMessages {
        @Comment("Generic Command Messages")
        private GenericCommands genericCommands = new GenericCommands();

        @Comment("\n/totemguard check")
        private CheckCommand checkCommand = new CheckCommand();

        @Comment("\n/totemguard profile")
        private ProfileCommand profileCommand = new ProfileCommand();

        @Comment("\n/totemguard database")
        private DatabaseCommand databaseCommand = new DatabaseCommand();

        @Comment("\n/totemguard clearlogs")
        private ClearLogsCommand clearLogsCommand = new ClearLogsCommand();

        @Configuration
        @Getter
        public static class GenericCommands {
            private String specifyPlayer = "%prefix% &cPlease specify a player.";
            private String pluginReloaded = "%prefix% &aThe plugin has been reloaded!";
            private String targetNeverJoined = "%prefix% &cTarget has never joined the server.";
            private String noDatabasePlayerFound = "%prefix% &cNo database player found for %player%.";
        }

        @Configuration
        @Getter
        public static class CheckCommand {
            private String targetCannotBeChecked = "%prefix% &cTarget cannot be checked.";
            private String targetOnCooldown = "%prefix% &cTarget is on cooldown for %cooldown%ms.";
            private String playerNotSurvival = "%prefix% &cTarget must be in survival mode to use this command.";
            private String playerInvulnerable = "%prefix% &cTarget is invulnerable.";
            private String playerNoTotem = "%prefix% &cTarget does not have a totem in their hands.";
            private String targetNoDamage = "%prefix% &cTarget did not receive any damage. Are they protected by a plugin or in a safe zone?";
            private String targetPassed = "%prefix% &a%player% has successfully passed the check.";
        }

        @Configuration
        @Getter
        public static class ProfileCommand {

            private String loadingProfile = "%prefix% &7Loading profile for %player%...";

            @Comment("\nProfile Format: The format for the message returned when checking a player's profile.")
            private ProfileFormat profileFormat = new ProfileFormat();

            @Configuration
            @Getter
            public static class ProfileFormat {
                private String profileFormat = """
                        &6&lTotemGuard Profile
                        &7&lPlayer: &6%player%
                        &7&lSafety Status: %safety_status%
                        &7&lTotal Alerts: &6%total_alerts%
                        &7&lTotal Punishments: &6%total_punishments%
                        &7&lLoad Time: &6%load_time%ms
                        
                        &6&l> Alert Summary <
                        %alert_summary%
                        
                        &6&l> Punishment Summary <
                        %punishment_summary%""";

                @Comment("\nProfile Alert Summary: The format for the alert summary in the profile. Will replace %alert_summary%.")
                private String alertSummary = "&8- &7&l%check_name% &6%violations%x";

                @Comment("\nProfile Punishment Summary: The format for the punishment summary in the profile. Will replace %punishment_summary%.")
                private String punishmentSummary = "&8- &7Punished for &6&l%check_name% &7on &6%date%";

                @Comment("\nProfile No Alerts Found: The message returned when no alerts are found. Will replace %alert_summary%.")
                private String noAlertsFound = "&7&o No alerts found.";

                @Comment("\nProfile No Punishments Found: The message returned when no punishments are found. Will replace %punishment_summary%.")
                private String noPunishmentsFound = "&7&o No punishments found.";

                @Comment("\nMessage to be added when more than 3 punishments are found.")
                private String showingLastPunishments = "&7&oShowing the last 3 punishments:";

                private String andMoreToBeDisplayed = "&7&o... and more not displayed";
            }
        }

        @Configuration
        @Getter
        public static class DatabaseCommand {
            @Comment("Message when the database clearing has started.")
            private String clearingStarted = "%prefix% &aDatabase clearing started...";

            @Comment("\nMessage when the database trimming has started.")
            private String trimmingStarted = "%prefix% &aDatabase trimming started...";

            @Comment("\nInvalid confirmation code has been provided.")
            private String invalidConfirmationCode = "%prefix% &cInvalid code. Please use the code provided.";

            @Comment("\nAction Confirmation Format")
            private ActionConfirmationFormat actionConfirmationFormat = new ActionConfirmationFormat();

            @Configuration
            @Getter
            public static class ActionConfirmationFormat {
                private String actionConfirmationFormat = """
                        &c&l[WARNING]: &7You are about to %action% the database.
                        &7This action is irreversible.
                        
                        &7Type: %command%
                        &7 or click %confirm_button% to confirm.
                        """;

                private String confirmButton = "&6&l[CONFIRM]";
                private String confirmHover = "&7Click to run %command%";
                private String confirmCommand = "/totemguard database %action% %code%";
            }
        }

        @Configuration
        @Getter
        public static class ClearLogsCommand {
            @Comment("Message when the logs are starting to get cleared.")
            private String clearingLogs = "%prefix% &7Clearing logs...";

            @Comment("\nMessage when the logs have been cleared.")
            private String logsCleared = "%prefix% &aCleared %amount% logs for %player% in %duration%ms.";
        }
    }
}