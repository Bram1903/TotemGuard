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

package com.deathmotion.totemguard.api.fleet;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Cross-fleet cache and pub/sub backed by TotemGuard's Redis connection. Only attached
 * while Redis is up. Every operation is best-effort, transport failures are swallowed
 * and surface as {@link Optional#empty()} or {@code false}. Keys are caller-namespaced
 * (the loader uses {@code totemguard:loader:*}) and values are opaque byte arrays.
 */
public interface FleetCache {

    /**
     * GET, returning bytes if the key exists and has not expired.
     */
    @NotNull Optional<byte[]> get(@NotNull String key);

    /**
     * SET with a hard positive TTL.
     */
    void put(@NotNull String key, byte @NotNull [] value, @NotNull Duration ttl);

    /**
     * DEL, no-op if absent.
     */
    void delete(@NotNull String key);

    /**
     * EXISTS without fetching the value.
     */
    boolean exists(@NotNull String key);

    /**
     * Read a Redis HASH as a string map, empty when absent.
     */
    @NotNull Map<String, String> getHash(@NotNull String key);

    /**
     * Atomically write all fields of {@code value} to a HASH and apply a TTL. Existing
     * fields not in {@code value} are deleted.
     */
    void putHash(@NotNull String key, @NotNull Map<String, String> value, @NotNull Duration ttl);

    /**
     * SCAN keys matching {@code prefix*}, returning up to {@code limit}. Implementations
     * may return fewer, so do not rely on completeness for tight time budgets.
     */
    @NotNull List<String> scanKeys(@NotNull String prefix, int limit);

    /**
     * Acquire a Redis-backed distributed lock (SET NX EX), or empty if another instance
     * holds it. The lock auto-expires at {@code ttl}, refresh for long operations.
     */
    @NotNull Optional<FleetLock> tryLock(@NotNull String key, @NotNull Duration ttl);

    /**
     * Fire-and-forget publish on a topic. Best-effort, peers not currently subscribed
     * will miss the message.
     */
    void publish(@NotNull String topic, byte @NotNull [] payload);

    /**
     * Subscribe to a topic. The returned {@link AutoCloseable} cancels the subscription.
     * The handler runs on a Redis I/O thread and must not block.
     */
    @NotNull AutoCloseable subscribe(@NotNull String topic, @NotNull Consumer<byte[]> handler);

    /**
     * Stable id for this TotemGuard instance, used as sender id and lock holder.
     */
    @NotNull UUID instanceId();

    /**
     * Best-effort Redis connection state. When {@code false}, fall back to the file-only
     * cache path.
     */
    boolean isHealthy();
}
