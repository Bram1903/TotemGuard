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

package com.deathmotion.totemguard.api.network;

import org.jetbrains.annotations.NotNull;

/**
 * Read-only view of the cross-server network state shared via Redis.
 * <p>
 * All values are best-effort snapshots maintained by a heartbeat loop and
 * Redis pub/sub. They reflect what this server has observed within the last
 * heartbeat window; a backend that has crashed without a clean shutdown will
 * still appear in the count for up to ~30 seconds before the stale-server
 * sweep removes it.
 * <p>
 * When Redis is unreachable {@link #isConnected()} returns {@code false} and
 * counts collapse to local-only state ({@link #getConnectedServerCount()}
 * returns {@code 1}, {@link #getTrackedPlayerCount()} returns the local
 * online-player count visible to TotemGuard).
 */
public interface NetworkRepository {

    /**
     * Number of TotemGuard backends currently active in the fleet, including this one.
     * Always at least {@code 1}.
     */
    int getConnectedServerCount();

    /**
     * Total number of players tracked by TotemGuard across the whole fleet.
     */
    int getTrackedPlayerCount();

    /**
     * Display name of this server as advertised to other backends. Comes from
     * {@code config.yml} and is what other backends print in alert messages.
     */
    @NotNull String getLocalServerName();

    /**
     * Whether this server's Redis connection is open. When {@code false} the
     * other accessors fall back to local-only data.
     */
    boolean isConnected();
}
