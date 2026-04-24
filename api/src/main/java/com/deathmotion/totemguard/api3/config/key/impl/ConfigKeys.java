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

public final class ConfigKeys {

    public static final ConfigValueKey<String> SERVER =
            ConfigValueKey.required(
                    "server",
                    "default"
            );

    public static final ConfigValueKey<String> COMMANDS_BASE =
            ConfigValueKey.required(
                    "commands.base",
                    "totemguard"
            );

    public static final ConfigValueKey<String> COMMAND_ALIASES =
            ConfigValueKey.required(
                    "commands.aliases",
                    "tg"
            );

    public static final ConfigValueKey<Boolean> REDIS_ENABLED =
            ConfigValueKey.required(
                    "redis.enabled",
                    false
            );

    public static final ConfigValueKey<String> REDIS_HOST =
            ConfigValueKey.required(
                    "redis.host",
                    "localhost"
            );

    public static final ConfigValueKey<Integer> REDIS_PORT =
            ConfigValueKey.required(
                    "redis.port",
                    6379
            );

    public static final ConfigValueKey<String> REDIS_USERNAME =
            ConfigValueKey.required(
                    "redis.username",
                    "default"
            );

    public static final ConfigValueKey<String> REDIS_PASSWORD =
            ConfigValueKey.required(
                    "redis.password",
                    "yourPassword"
            );

    public static final ConfigValueKey<String> REDIS_MESSAGING_CHANNEL =
            ConfigValueKey.required(
                    "redis.messaging.channel",
                    "totemguard"
            );

    public static final ConfigValueKey<Boolean> REDIS_MESSAGING_SEND_ALERTS =
            ConfigValueKey.required(
                    "redis.messaging.send-alerts",
                    true
            );

    public static final ConfigValueKey<Boolean> REDIS_MESSAGING_RECEIVE_ALERTS =
            ConfigValueKey.required(
                    "redis.messaging.receive-alerts",
                    true
            );

    public static final ConfigValueKey<Boolean> VPN_ENABLED =
            ConfigValueKey.required(
                    "anti-vpn.enabled",
                    true
            );

    public static final ConfigValueKey<String> VPN_PROVIDER =
            ConfigValueKey.required(
                    "anti-vpn.provider",
                    "IPRisk"
            );

    public static final ConfigValueKey<String> VPN_API_KEY =
            ConfigValueKey.required(
                    "anti-vpn.api-key",
                    ""
            );

    public static final ConfigValueKey<Boolean> VPN_BLOCK =
            ConfigValueKey.required(
                    "anti-vpn.block",
                    true
            );

    public static final ConfigValueKey<Boolean> DATABASE_ENABLED =
            ConfigValueKey.required(
                    "database.enabled",
                    false
            );

    public static final ConfigValueKey<String> DATABASE_HOST =
            ConfigValueKey.required(
                    "database.host",
                    "localhost"
            );

    public static final ConfigValueKey<Integer> DATABASE_PORT =
            ConfigValueKey.required(
                    "database.port",
                    3306
            );

    public static final ConfigValueKey<String> DATABASE_DATABASE =
            ConfigValueKey.required(
                    "database.database",
                    "totemguard"
            );

    public static final ConfigValueKey<String> DATABASE_USERNAME =
            ConfigValueKey.required(
                    "database.username",
                    "root"
            );

    public static final ConfigValueKey<String> DATABASE_PASSWORD =
            ConfigValueKey.required(
                    "database.password",
                    "password"
            );

    public static final ConfigValueKey<String> DATABASE_PARAMETERS =
            ConfigValueKey.required(
                    "database.parameters",
                    ""
            );

    public static final ConfigValueKey<Integer> DATABASE_RETENTION_ALERT_DAYS =
            ConfigValueKey.required(
                    "database.retention.alerts-days",
                    0
            );

    public static final ConfigValueKey<Integer> DATABASE_RETENTION_VPN_DAYS =
            ConfigValueKey.required(
                    "database.retention.vpn-cache-days",
                    30
            );

    private ConfigKeys() {
    }
}
