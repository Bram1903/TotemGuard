/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.api3.config.key.impl;

import com.deathmotion.totemguard.api3.config.key.ConfigValueKey;

public final class MessagesKeys {

    public static final ConfigValueKey<String> PREFIX =
            ConfigValueKey.required(
                    "prefix",
                    "&6&lTG &8»"
            );

    public static final ConfigValueKey<String> ALERTS_MESSAGE =
            ConfigValueKey.required(
                    "alerts.message",
                    "%prefix% &e%tg_player%&7 failed &6%tg_check% &7VL[&6%tg_check_violations%/∞&7]"
            );

    public static final ConfigValueKey<String> ALERTS_DEBUG =
            ConfigValueKey.required(
                    "alerts.debug",
                    " &7(&8%tg_check_debug%&7)"
            );

    public static final ConfigValueKey<String> ALERTS_HOVER =
            ConfigValueKey.required(
                    "alerts.hover",
                    "&#e7dec4Client Version: &#FEE067%tg_player_version% &#8f7440| &#e7dec4Client Brand: &#FEE067%tg_player_brand%\n" +
                            "\n" +
                            "&#e7dec4Player: &#FEE067%tg_player%\n" +
                            "&#e7dec4Ping: &#FEE067(K: %k_ping%ms &#8f7440| &#FEE067T: %t_ping%ms)\n" +
                            "\n" +
                            "&#e7dec4Check: &#FEE067%tg_check_name%\n" +
                            "&#e7dec4Description: &#FEE067%tg_check_description%\n" +
                            "&#e7dec4Server: &#FEE067%tg_server%\n" +
                            "\n" +
                            "%tg_check_details%\n" +
                            "\n" +
                            "&#e7dec4Click to &#fbaf00teleport &#e7dec4to &#FEE067%tg_player%&#e7dec4."
            );

    public static final ConfigValueKey<String> ALERTS_COMMAND =
            ConfigValueKey.required(
                    "alerts.command",
                    "/tp %tg_player%"
            );

    public static final ConfigValueKey<String> ALERTS_ENABLED =
            ConfigValueKey.required(
                    "alerts.enabled",
                    "%prefix% &aAlerts enabled"
            );

    public static final ConfigValueKey<String> ALERTS_DISABLED =
            ConfigValueKey.required(
                    "alerts.disabled",
                    "%prefix% &cAlerts disabled"
            );

    public static final ConfigValueKey<String> RELOAD =
            ConfigValueKey.required(
                    "reload",
                    "%prefix% &aTotemGuard reloaded successfully!"
            );

    public static final ConfigValueKey<String> ANTI_VPN_ALERT =
            ConfigValueKey.required(
                    "anti-vpn.alert",
                    "%prefix% &6%tg_player% &7tried to join using a VPN"
            );

    public static final ConfigValueKey<String> ANTI_VPN_KICK_MESSAGE =
            ConfigValueKey.required(
                    "anti-vpn.kick_message",
                    "&#fbaf00%tg_player% &7has been kicked for using a VPN"
            );

    private MessagesKeys() {
    }
}
