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

package com.deathmotion.totemguard.common.database;

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.key.impl.ConfigKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import lombok.Getter;

/**
 * Snapshot of database-related configuration.
 *
 * <p>Pool sizing, batching cadence and sweep scheduling are intentionally
 * not surfaced in config.yml — the hardcoded defaults are tuned to behave
 * well from tiny servers up to million-alert/day deployments. Only the
 * connection details and retention windows are user-facing.</p>
 */
@Getter
public final class DatabaseOptions {

    // Connection pool — safe defaults for any server size.
    public static final int POOL_MAX_SIZE = 10;
    public static final int POOL_MIN_IDLE = 2;
    public static final int POOL_CONNECTION_TIMEOUT_MS = 5_000;
    public static final int POOL_IDLE_TIMEOUT_MS = 60_000;
    public static final int POOL_MAX_LIFETIME_MS = 1_800_000;

    // Alert writer batching — 100 rows or 1s, whichever comes first.
    public static final int BATCH_MAX_SIZE = 100;
    public static final int BATCH_FLUSH_INTERVAL_MS = 1_000;
    public static final int BATCH_QUEUE_CAPACITY = 10_000;

    private final boolean enabled;
    private final String serverName;

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String parameters;

    private final int retentionAlertDays;
    private final int retentionVpnDays;

    public DatabaseOptions() {
        Config config = TGPlatform.getInstance().getConfigRepository().config(ConfigFile.CONFIG);

        this.enabled = config.getBoolean(ConfigKeys.DATABASE_ENABLED);
        this.serverName = config.getString(ConfigKeys.SERVER);

        this.host = config.getString(ConfigKeys.DATABASE_HOST);
        this.port = config.getInt(ConfigKeys.DATABASE_PORT);
        this.database = config.getString(ConfigKeys.DATABASE_DATABASE);
        this.username = config.getString(ConfigKeys.DATABASE_USERNAME);
        this.password = config.getString(ConfigKeys.DATABASE_PASSWORD);
        this.parameters = config.getString(ConfigKeys.DATABASE_PARAMETERS);

        this.retentionAlertDays = config.getInt(ConfigKeys.DATABASE_RETENTION_ALERT_DAYS);
        this.retentionVpnDays = config.getInt(ConfigKeys.DATABASE_RETENTION_VPN_DAYS);
    }
}
