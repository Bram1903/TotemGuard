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

package com.deathmotion.totemguard.common.config.view;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.common.config.key.ConfigKeys;
import com.deathmotion.totemguard.common.config.schema.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Internal typed view of {@code config.yml}. Owns the typed snapshots for each domain
 * subsection so consumers (database, redis, anti-vpn, commands) pull config values
 * through this view rather than constructing their own readers.
 */
public final class ConfigView {

    private final int version;
    private final String server;
    private final CommandsOptions commands;
    private final RedisOptions redis;
    private final DatabaseOptions database;
    private final AntiVpnOptions antiVpn;
    private final UpdateCheckerOptions updateChecker;
    private final EntitySpoofingOptions entitySpoofing;
    private final Set<String> developerOverrides;

    public ConfigView(Config config) {
        this.version = config.version();
        this.server = config.getString(ConfigKeys.SERVER);
        this.commands = new CommandsOptions(
                config.getString(ConfigKeys.COMMANDS_BASE),
                config.getStringList(ConfigKeys.COMMAND_ALIASES)
        );
        this.redis = new RedisOptions(
                config.getBoolean(ConfigKeys.REDIS_ENABLED),
                config.getString(ConfigKeys.REDIS_HOST),
                config.getInt(ConfigKeys.REDIS_PORT),
                config.getString(ConfigKeys.REDIS_USERNAME),
                config.getString(ConfigKeys.REDIS_PASSWORD),
                new RedisOptions.MessagingOptions(
                        config.getString(ConfigKeys.REDIS_MESSAGING_CHANNEL),
                        config.getBoolean(ConfigKeys.REDIS_MESSAGING_SEND_ALERTS),
                        config.getBoolean(ConfigKeys.REDIS_MESSAGING_RECEIVE_ALERTS)
                )
        );
        this.database = new DatabaseOptions(
                config.getBoolean(ConfigKeys.DATABASE_ENABLED),
                this.server,
                config.getString(ConfigKeys.DATABASE_HOST),
                config.getInt(ConfigKeys.DATABASE_PORT),
                config.getString(ConfigKeys.DATABASE_DATABASE),
                config.getString(ConfigKeys.DATABASE_USERNAME),
                config.getString(ConfigKeys.DATABASE_PASSWORD),
                config.getString(ConfigKeys.DATABASE_PARAMETERS),
                config.getInt(ConfigKeys.DATABASE_RETENTION_ALERT_DAYS),
                config.getInt(ConfigKeys.DATABASE_RETENTION_VPN_DAYS)
        );
        this.antiVpn = new AntiVpnOptions(
                config.getBoolean(ConfigKeys.VPN_ENABLED),
                config.getString(ConfigKeys.VPN_PROVIDER),
                config.getString(ConfigKeys.VPN_API_KEY),
                config.getBoolean(ConfigKeys.VPN_BLOCK)
        );
        this.updateChecker = new UpdateCheckerOptions(
                config.getBoolean(ConfigKeys.UPDATE_CHECKER_ENABLED),
                config.getBoolean(ConfigKeys.UPDATE_CHECKER_NOTIFY_ON_JOIN)
        );
        this.entitySpoofing = new EntitySpoofingOptions(
                config.getBoolean(ConfigKeys.ENTITY_SPOOFING_HEALTH),
                config.getBoolean(ConfigKeys.ENTITY_SPOOFING_ABSORPTION)
        );
        List<String> overrides = config.getStringList(ConfigKeys.DEVELOPER_OVERRIDES);
        this.developerOverrides = overrides.isEmpty() ? Set.of() : Set.copyOf(new HashSet<>(overrides));
    }

    public int version() {
        return version;
    }

    public @NotNull String server() {
        return server;
    }

    public @NotNull CommandsOptions commands() {
        return commands;
    }

    public @NotNull RedisOptions redis() {
        return redis;
    }

    public @NotNull DatabaseOptions database() {
        return database;
    }

    public @NotNull AntiVpnOptions antiVpn() {
        return antiVpn;
    }

    public @NotNull UpdateCheckerOptions updateChecker() {
        return updateChecker;
    }

    public @NotNull EntitySpoofingOptions entitySpoofing() {
        return entitySpoofing;
    }

    public boolean hasDeveloperOverride(@NotNull String flag) {
        return developerOverrides.contains(flag);
    }
}
