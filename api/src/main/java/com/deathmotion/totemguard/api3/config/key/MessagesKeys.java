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

package com.deathmotion.totemguard.api3.config.key;

/**
 * Typed paths into {@code messages.yml}.
 * <p>
 * Defaults live in the bundled {@code messages.yml} resource.
 */
public final class MessagesKeys {

    public static final ConfigKey<String> PREFIX = ConfigKey.string("prefix");

    public static final ConfigKey<String> ALERTS_MESSAGE = ConfigKey.string("alerts.message");
    public static final ConfigKey<String> ALERTS_HOVER = ConfigKey.string("alerts.hover");
    public static final ConfigKey<String> ALERTS_COMMAND = ConfigKey.string("alerts.command");
    public static final ConfigKey<String> ALERTS_ENABLED = ConfigKey.string("alerts.enabled");
    public static final ConfigKey<String> ALERTS_DISABLED = ConfigKey.string("alerts.disabled");

    public static final ConfigKey<String> RELOAD = ConfigKey.string("reload");

    public static final ConfigKey<String> ANTI_VPN_ALERT = ConfigKey.string("anti-vpn.alert");
    public static final ConfigKey<String> ANTI_VPN_KICK = ConfigKey.string("anti-vpn.kick");

    private MessagesKeys() {
    }
}
