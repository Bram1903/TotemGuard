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

package com.deathmotion.totemguard.common.config.key;

import com.deathmotion.totemguard.api3.config.key.ConfigKey;

import java.util.List;

/**
 * Typed paths into {@code config.yml}.
 * <p>
 * Defaults are not declared here; they live in the bundled {@code config.yml} resource.
 * These constants are an internal implementation detail of TotemGuard's bundled YAML —
 * they are intentionally not part of the published API. External code that needs a value
 * should call {@link com.deathmotion.totemguard.api3.config.Config#getString(String)}
 * (or one of its sibling path-based accessors) directly with the dotted path.
 */
public final class ConfigKeys {

    public static final ConfigKey<String> SERVER = ConfigKey.string("server");

    public static final ConfigKey<String> COMMANDS_BASE = ConfigKey.string("commands.base");
    public static final ConfigKey<List<String>> COMMAND_ALIASES = ConfigKey.stringList("commands.aliases");

    public static final ConfigKey<Boolean> REDIS_ENABLED = ConfigKey.bool("redis.enabled");
    public static final ConfigKey<String> REDIS_HOST = ConfigKey.string("redis.host");
    public static final ConfigKey<Integer> REDIS_PORT = ConfigKey.integer("redis.port");
    public static final ConfigKey<String> REDIS_USERNAME = ConfigKey.string("redis.username");
    public static final ConfigKey<String> REDIS_PASSWORD = ConfigKey.string("redis.password");
    public static final ConfigKey<String> REDIS_MESSAGING_CHANNEL = ConfigKey.string("redis.messaging.channel");
    public static final ConfigKey<Boolean> REDIS_MESSAGING_SEND_ALERTS = ConfigKey.bool("redis.messaging.send-alerts");
    public static final ConfigKey<Boolean> REDIS_MESSAGING_RECEIVE_ALERTS = ConfigKey.bool("redis.messaging.receive-alerts");

    public static final ConfigKey<Boolean> UPDATE_CHECKER_ENABLED = ConfigKey.bool("update-checker.enabled");
    public static final ConfigKey<Boolean> UPDATE_CHECKER_NOTIFY_ON_JOIN = ConfigKey.bool("update-checker.notify-on-join");

    public static final ConfigKey<Boolean> ENTITY_SPOOFING_HEALTH = ConfigKey.bool("entity-spoofing.health");
    public static final ConfigKey<Boolean> ENTITY_SPOOFING_ABSORPTION = ConfigKey.bool("entity-spoofing.absorption");

    public static final ConfigKey<Boolean> VPN_ENABLED = ConfigKey.bool("anti-vpn.enabled");
    public static final ConfigKey<String> VPN_PROVIDER = ConfigKey.string("anti-vpn.provider");
    public static final ConfigKey<String> VPN_API_KEY = ConfigKey.string("anti-vpn.api-key");
    public static final ConfigKey<Boolean> VPN_BLOCK = ConfigKey.bool("anti-vpn.block");

    public static final ConfigKey<Boolean> DATABASE_ENABLED = ConfigKey.bool("database.enabled");
    public static final ConfigKey<String> DATABASE_HOST = ConfigKey.string("database.host");
    public static final ConfigKey<Integer> DATABASE_PORT = ConfigKey.integer("database.port");
    public static final ConfigKey<String> DATABASE_DATABASE = ConfigKey.string("database.database");
    public static final ConfigKey<String> DATABASE_USERNAME = ConfigKey.string("database.username");
    public static final ConfigKey<String> DATABASE_PASSWORD = ConfigKey.string("database.password");
    public static final ConfigKey<String> DATABASE_PARAMETERS = ConfigKey.string("database.parameters");
    public static final ConfigKey<Integer> DATABASE_RETENTION_ALERT_DAYS = ConfigKey.integer("database.retention.alerts-days");
    public static final ConfigKey<Integer> DATABASE_RETENTION_VPN_DAYS = ConfigKey.integer("database.retention.vpn-cache-days");

    public static final ConfigKey<List<String>> DEVELOPER_OVERRIDES = ConfigKey.stringList("developer-overrides");

    private ConfigKeys() {
    }
}
