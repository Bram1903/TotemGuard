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
 * Read-only view of cross-server state shared via Redis. Values are heartbeat-driven
 * snapshots, a crashed backend may linger in counts for up to about 30 seconds. When
 * Redis is unreachable, accessors collapse to local-only state.
 */
public interface NetworkRepository {

    /**
     * Active TotemGuard backends in the fleet including this one, counted from Redis
     * heartbeats. Always at least {@code 1}, collapses to {@code 1} when Redis is offline.
     */
    int getConnectedServerCount();

    /**
     * Sum of player counts reported by every backend in the fleet. Mirrors what staff see
     * in cross-server alerts, falls back to the local player count when Redis is offline.
     */
    int getTrackedPlayerCount();

    /**
     * This backend's friendly name from {@code config.yml} ({@code server-name}). Used as
     * the {@code serverName} stamped on alerts and punishments written from this node.
     */
    @NotNull String getLocalServerName();

    /**
     * Whether the Lettuce Redis connection is currently open. Same value the rest of the
     * repository uses to decide between fleet-aware and local-only behavior.
     */
    boolean isConnected();
}
