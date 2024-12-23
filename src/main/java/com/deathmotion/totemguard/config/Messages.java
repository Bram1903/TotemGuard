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

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@SuppressWarnings("FieldMayBeFinal")
@Configuration
@Getter
public class Messages {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Comment("Prefix: The prefix for all messages.")
    private Component Prefix = MINI_MESSAGE.deserialize("<gold><bold>TG</bold></gold> <dark_gray>»</dark_gray>");

    @Comment("\nAlert Format: The format for all alerts.")
    private Component AlertFormat = MINI_MESSAGE.deserialize("<click:run_command:'/tp %player%'><hover:show_text:'<gray>TPS: </gray><gold>%tps%</gold><dark_gray> |</dark_gray><gray> Client Version: </gray><gold>%client_version%</gold><dark_gray> |</dark_gray><gray> Client Brand: </gray><gold>%client_brand%</gold><br><br><gray>Player: </gray><gold>%player%</gold><br><gray>Ping: </gray><gold>%ping%ms</gold><br><br><gray>Check: </gray><gold>%check_name%</gold><br><gray>Description: </gray><gold>%check_description</gold><br><gray>Server: </gray><gold>%server%</gold><br><br>%check_details%<br><br><gray>Click to </gray><gold>teleport </gold><gray>to %player%.'>%prefix% <yellow>%player%</yellow><gray> failed </gray><gold>%check_name%</gold><white> </white><gray>VL[</gray><gold>%violations%/%max_violations%</gold><gray>]\n");

    @Comment("\nAlerts Enabled: Message when alerts are enabled.")
    private Component AlertsEnabled = MINI_MESSAGE.deserialize("%prefix% <green>Alerts enabled");
    private Component AlertsDisabled = MINI_MESSAGE.deserialize("%prefix% <red>Alerts disabled");
    private Component AlertBrand = MINI_MESSAGE.deserialize("%prefix% <gold>%player%</gold> joined using: <gold>%client_brand%</gold>");
}



