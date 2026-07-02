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

import com.deathmotion.totemguard.api.config.key.ConfigKey;

import java.util.List;

public final class ConfigKeys {

    public static final ConfigKey<String> SERVER = ConfigKey.string("server");

    public static final ConfigKey<String> COMMANDS_BASE = ConfigKey.string("commands.base");
    public static final ConfigKey<List<String>> COMMAND_ALIASES = ConfigKey.stringList("commands.aliases");

    public static final ConfigKey<Boolean> REDIS_ENABLED = ConfigKey.bool("redis.enabled");
    public static final ConfigKey<Boolean> REDIS_CLUSTER = ConfigKey.bool("redis.cluster");
    public static final ConfigKey<Integer> REDIS_OFFLINE_GRACE_MILLIS = ConfigKey.integer("redis.offline-grace-millis");
    public static final ConfigKey<String> REDIS_HOST = ConfigKey.string("redis.host");
    public static final ConfigKey<Integer> REDIS_PORT = ConfigKey.integer("redis.port");
    public static final ConfigKey<String> REDIS_USERNAME = ConfigKey.string("redis.username");
    public static final ConfigKey<String> REDIS_PASSWORD = ConfigKey.string("redis.password");
    public static final ConfigKey<String> REDIS_MESSAGING_ALERTS_CHANNEL = ConfigKey.string("redis.messaging.alerts.channel");
    public static final ConfigKey<Boolean> REDIS_MESSAGING_ALERTS_SEND = ConfigKey.bool("redis.messaging.alerts.send");
    public static final ConfigKey<Boolean> REDIS_MESSAGING_ALERTS_RECEIVE = ConfigKey.bool("redis.messaging.alerts.receive");

    public static final ConfigKey<Boolean> UPDATE_CHECKER_ENABLED = ConfigKey.bool("update-checker.enabled");
    public static final ConfigKey<Boolean> UPDATE_CHECKER_NOTIFY_ON_JOIN = ConfigKey.bool("update-checker.notify-on-join");

    public static final ConfigKey<Boolean> ENTITY_SPOOFING_HEALTH = ConfigKey.bool("entity-spoofing.health");
    public static final ConfigKey<Boolean> ENTITY_SPOOFING_ABSORPTION = ConfigKey.bool("entity-spoofing.absorption");

    public static final ConfigKey<Boolean> BAN_ANIMATION_ENABLED = ConfigKey.bool("ban-animation.enabled");

    public static final ConfigKey<Boolean> TICK_SKIP_KEEP_ALIVE_VALIDATION = ConfigKey.bool("tick.skip-keep-alive-validation");

    public static final ConfigKey<Boolean> PHYSICS_ENGINE_ENABLED = ConfigKey.bool("physics-engine.enabled");

    public static final ConfigKey<String> PHYSICS_ENGINE_TOLERANCE = ConfigKey.string("physics-engine.tolerance");

    public static final ConfigKey<Boolean> PHYSICS_ENGINE_SETBACK = ConfigKey.bool("physics-engine.mitigation.setback");

    public static final ConfigKey<Boolean> PHYSICS_ENGINE_CLOSE_INVENTORY = ConfigKey.bool("physics-engine.mitigation.close-inventory");

    public static final ConfigKey<Boolean> PHYSICS_ENGINE_DEBUG = ConfigKey.bool("physics-engine.debug");

    public static final ConfigKey<Boolean> DEBUG_MODIFIER_KICK_ENABLED = ConfigKey.bool("debug-modifier-kick.enabled");

    public static final ConfigKey<String> TELEPORT_COMMAND = ConfigKey.string("teleport.command");

    public static final ConfigKey<Boolean> DATABASE_ENABLED = ConfigKey.bool("database.enabled");
    public static final ConfigKey<String> DATABASE_HOST = ConfigKey.string("database.host");
    public static final ConfigKey<Integer> DATABASE_PORT = ConfigKey.integer("database.port");
    public static final ConfigKey<String> DATABASE_DATABASE = ConfigKey.string("database.database");
    public static final ConfigKey<String> DATABASE_USERNAME = ConfigKey.string("database.username");
    public static final ConfigKey<String> DATABASE_PASSWORD = ConfigKey.string("database.password");
    public static final ConfigKey<String> DATABASE_PARAMETERS = ConfigKey.string("database.parameters");
    public static final ConfigKey<Integer> DATABASE_RETENTION_ALERT_DAYS = ConfigKey.integer("database.retention.alerts-days");

    public static final ConfigKey<List<String>> DEVELOPER_OVERRIDES = ConfigKey.stringList("developer-overrides");

    private ConfigKeys() {
    }
}
