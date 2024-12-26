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
    @Comment("""
            Which text formatter to use (MINIMESSAGE, LEGACY)
            (&#084cfbc LEGACY - <color:#084cfbc> MINIMESSAGE)""")
    private Formatter format = Formatter.MINIMESSAGE;

    @Comment("\nPrefix: The prefix for all messages.")
    private String prefix = "<gold><bold>TG</bold></gold> <dark_gray>»</dark_gray>";

    @Comment("\nAlert Format: The format for all alerts.")
    private AlertFormat alertFormat = new AlertFormat();

    @Comment("\n")
    private String alertsEnabled = "%prefix% <green>Alerts enabled";
    private String alertsDisabled = "%prefix% <red>Alerts disabled";
    private String alertBrand = "%prefix% <gold>%player%</gold> joined using: <gold>%brand%</gold>";

    @Configuration
    @Getter
    public static class AlertFormat {
        @Comment("Alert Format: The format for all alerts.")
        private String alertFormat = "%prefix% <yellow>%player%</yellow><gray> failed </gray><gold>%check_name%</gold><white> </white><gray>VL[</gray><gold>%violations%/%max_violations%</gold><gray>]";

        @Comment("\nAlert Format Console: The format for all alerts in console.")
        private String alertFormatConsole = "%prefix% <yellow>%player%</yellow><gray> failed </gray><gold>%check_name%</gold><white> </white><gray>VL[</gray><gold>%violations%/%max_violations%</gold><gray>]";

        @Comment("\nAlert Hover Format: The format for all alerts when hovered over.")
        private String alertHoverMessage = """
                <gray>TPS: </gray><gold>%tps%</gold><dark_gray> |</dark_gray><gray> Client Version: </gray><gold>%client_version%</gold><dark_gray> |</dark_gray><gray> Client Brand: </gray><gold>%client_brand%</gold>
                
                <gray>Player: </gray><gold>%player%</gold>
                <gray>Ping: </gray><gold>%ping%ms</gold>
                
                <gray>Check: </gray><gold>%check_name%</gold>
                <gray>Description: </gray><gold>%check_description%</gold>
                <gray>Server: </gray><gold>%server%</gold>
                
                %check_details%
                
                <gray>Click to </gray><gold>teleport </gold><gray>to %player%.""";

        @Comment("\nAlert Click Command: The command to run when the alert is clicked.")
        private String alertClickCommand = "/tp %player%";

        @Comment("\nCheck Details Color: The color for the alert details.")
        private CheckDetailsColor checkDetailsColor = new CheckDetailsColor();

        @Configuration
        @Getter
        public static class CheckDetailsColor {
            @Comment("Main Color: The main color for the check details.")
            private String main = "<gray>";

            @Comment("\nSecondary Color: The secondary color for the check details.")
            private String secondary = "<gold>";
        }
    }
}



