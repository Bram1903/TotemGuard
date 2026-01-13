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

package com.deathmotion.totemguard.api.config.key.impl;

import com.deathmotion.totemguard.api.config.key.ConfigValueKey;

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

    private MessagesKeys() {
    }
}
