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

    @Comment("")
    private String specifyPlayer = "%prefix% &cPlease specify a player.";

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
}