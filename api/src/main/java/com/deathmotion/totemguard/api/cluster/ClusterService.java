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

package com.deathmotion.totemguard.api.cluster;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Cross-fleet coordination primitives backed by TotemGuard's shared Redis connection. Obtain
 * via {@link com.deathmotion.totemguard.api.TotemGuardAPI#getCluster()}.
 *
 * <p>Everything degrades gracefully when Redis is not connected: leases are granted locally
 * (so a standalone node still proceeds) and pub/sub becomes a no-op. Use {@link #isConnected()}
 * when behaviour should differ between a real fleet and a single node.
 */
public interface ClusterService {

    /**
     * Whether a shared coordinator (Redis) is currently connected. When {@code false}, this
     * node cannot coordinate with others.
     */
    boolean isConnected();

    /**
     * Attempts to acquire a named, fleet-wide lease for single-owner coordination, such as
     * picking which node runs a shared external connection. With Redis connected, at most one
     * node across the fleet holds a given lease at a time. Without it, the lease is granted
     * locally so a standalone node always succeeds.
     *
     * @param name a stable lease identifier shared by every node competing for it
     * @param ttl  how long the lease is held before it must be renewed
     * @return the held lease, or empty when another node currently holds it
     */
    @NotNull Optional<ClusterLease> acquireLease(@NotNull String name, @NotNull Duration ttl);

    /**
     * Publishes a message to every node subscribed to {@code channel}. Best-effort, and a
     * no-op when Redis is not connected. Peers not currently subscribed miss the message.
     */
    void publish(@NotNull String channel, byte @NotNull [] message);

    /**
     * Subscribes to {@code channel}. The handler runs on a Redis I/O thread and must not
     * block. The returned {@link AutoCloseable} cancels the subscription. The subscription
     * re-attaches automatically across Redis reconnects.
     */
    @NotNull AutoCloseable subscribe(@NotNull String channel, @NotNull Consumer<byte @NotNull []> handler);
}
