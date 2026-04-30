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

    public static final ConfigKey<String> UPDATE_AVAILABLE = ConfigKey.string("update-checker.available");

    public static final ConfigKey<String> ANTI_VPN_ALERT = ConfigKey.string("anti-vpn.alert");
    public static final ConfigKey<String> ANTI_VPN_KICK = ConfigKey.string("anti-vpn.kick");

    public static final ConfigKey<String> GENERAL_PLAYER_ONLY = ConfigKey.string("general.player-only");
    public static final ConfigKey<String> GENERAL_PLAYER_NOT_FOUND = ConfigKey.string("general.player-not-found");
    public static final ConfigKey<String> GENERAL_PLAYER_DATA_MISSING = ConfigKey.string("general.player-data-missing");
    public static final ConfigKey<String> GENERAL_DATABASE_UNAVAILABLE = ConfigKey.string("general.database-unavailable");
    public static final ConfigKey<String> GENERAL_NO_RECORDS = ConfigKey.string("general.no-records");
    public static final ConfigKey<String> GENERAL_LOOKUP_FAILED = ConfigKey.string("general.lookup-failed");

    public static final ConfigKey<String> ROOT_VERSION = ConfigKey.string("commands.root.version");
    public static final ConfigKey<String> ROOT_GUI_OPEN_FAILED = ConfigKey.string("commands.root.gui-open-failed");

    public static final ConfigKey<String> CHECK_BACKEND_ONLY = ConfigKey.string("commands.check.backend-only");
    public static final ConfigKey<String> CHECK_ALREADY_CHECKING = ConfigKey.string("commands.check.already-checking");
    public static final ConfigKey<String> CHECK_ON_COOLDOWN = ConfigKey.string("commands.check.on-cooldown");
    public static final ConfigKey<String> CHECK_WRONG_GAMEMODE = ConfigKey.string("commands.check.wrong-gamemode");
    public static final ConfigKey<String> CHECK_INVULNERABLE = ConfigKey.string("commands.check.invulnerable");
    public static final ConfigKey<String> CHECK_NO_TOTEM = ConfigKey.string("commands.check.no-totem");
    public static final ConfigKey<String> CHECK_DAMAGE_FAILED = ConfigKey.string("commands.check.damage-failed");
    public static final ConfigKey<String> CHECK_FLAGGED = ConfigKey.string("commands.check.flagged");
    public static final ConfigKey<String> CHECK_PASSED = ConfigKey.string("commands.check.passed");

    public static final ConfigKey<String> MONITOR_SELF = ConfigKey.string("commands.monitor.self-monitor");
    public static final ConfigKey<String> MONITOR_BLOCKED = ConfigKey.string("commands.monitor.blocked");
    public static final ConfigKey<String> MONITOR_OPEN_FAILED = ConfigKey.string("commands.monitor.open-failed");

    public static final ConfigKey<String> HISTORY_OPEN_FAILED = ConfigKey.string("commands.history.open-failed");

    public static final ConfigKey<String> PROFILE_OPEN_FAILED = ConfigKey.string("commands.profile.open-failed");

    public static final ConfigKey<String> CLEARHISTORY_CLEARING = ConfigKey.string("commands.clearhistory.clearing");
    public static final ConfigKey<String> CLEARHISTORY_CLEARED = ConfigKey.string("commands.clearhistory.cleared");
    public static final ConfigKey<String> CLEARHISTORY_CLEAR_FAILED = ConfigKey.string("commands.clearhistory.clear-failed");

    public static final ConfigKey<String> DEBUG_UNKNOWN_OVERLAY = ConfigKey.string("commands.debug.unknown-overlay");
    public static final ConfigKey<String> DEBUG_NO_PERMISSION = ConfigKey.string("commands.debug.no-permission");
    public static final ConfigKey<String> DEBUG_ENABLED = ConfigKey.string("commands.debug.enabled");
    public static final ConfigKey<String> DEBUG_DISABLED = ConfigKey.string("commands.debug.disabled");

    private MessagesKeys() {
    }
}
