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
import org.jetbrains.annotations.NotNull;

public final class ConfigKeys {

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

    public static final ConfigValueKey<Boolean> CACHE_ENABLED =
            ConfigValueKey.required(
                    "cache.enabled",
                    true
            );

    public static final ConfigValueKey<Integer> CACHE_DATA_CHECKS =
            ConfigValueKey.required(
                    "cache.data.checks",
                    300
            );

    public static final ConfigValueKey<Integer> CACHE_PUNISH_QUEUE =
            ConfigValueKey.required(
                    "cache.data.punish-queue",
                    60
            );

    public static final @NotNull ConfigValueKey<Integer> CACHE_ALERTS_TOGGLE =
            ConfigValueKey.required(
                    "cache.data.alerts-toggle",
                    1800
            );

    public static final ConfigValueKey<Integer> CACHE_DATA_VPN =
            ConfigValueKey.required(
                    "cache.data.vpn-data",
                    1800
            );

    public static final ConfigValueKey<Integer> CACHE_LOCAL_MAX_ENTRIES =
            ConfigValueKey.required(
                    "cache.local.max-entries",
                    10000
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

    private ConfigKeys() {
    }
}