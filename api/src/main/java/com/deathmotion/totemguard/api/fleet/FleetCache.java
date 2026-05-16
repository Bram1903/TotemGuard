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
 * Cross-fleet cache + pub/sub provided by a TotemGuard instance to its loader. Backed
 * by the TotemGuard Redis connection; only attached after Redis is up. Detached when
 * Redis disconnects or TotemGuard shuts down.
 *
 * <p>Every operation is best-effort and side-effect-isolated. Failures must not bubble
 * up to the loader's critical path; implementations swallow transport errors and
 * return {@link Optional#empty()} or {@code false}.</p>
 *
 * <p>Keys are caller-namespaced. The loader uses the {@code totemguard:loader:*}
 * prefix. Values are opaque byte arrays so the loader chooses its own encoding
 * (JSON, raw jar bytes, etc).</p>
 */
public interface FleetCache {

    /**
     * GET a key, returning the raw bytes if present and the key has not expired.
     */
    @NotNull Optional<byte[]> get(@NotNull String key);

    /**
     * SET a key with a hard TTL. {@code ttl} must be positive.
     */
    void put(@NotNull String key, byte @NotNull [] value, @NotNull Duration ttl);

    /**
     * DEL a key. No-op if absent.
     */
    void delete(@NotNull String key);

    /**
     * EXISTS check without fetching the value.
     */
    boolean exists(@NotNull String key);

    /**
     * Read a Redis HASH as a string map. Returns empty when the key is absent.
     */
    @NotNull Map<String, String> getHash(@NotNull String key);

    /**
     * Atomically write all fields of {@code value} to a Redis HASH and apply a TTL.
     * Existing fields not present in {@code value} are deleted.
     */
    void putHash(@NotNull String key, @NotNull Map<String, String> value, @NotNull Duration ttl);

    /**
     * SCAN keys matching {@code prefix*}. Returns up to {@code limit} entries.
     * Implementations may return fewer; callers should not rely on completeness for
     * tight time budgets.
     */
    @NotNull List<String> scanKeys(@NotNull String prefix, int limit);

    /**
     * Try to acquire a Redis-backed distributed lock (SET NX EX). Returns empty if
     * another instance holds it. The lock auto-expires after {@code ttl}; callers
     * should refresh in long-running operations or accept the TTL as the upper bound.
     */
    @NotNull Optional<FleetLock> tryLock(@NotNull String key, @NotNull Duration ttl);

    /**
     * Publish a fire-and-forget message on a fleet topic. Delivery is best-effort;
     * peers not currently subscribed (e.g. starting up) will miss the message.
     */
    void publish(@NotNull String topic, byte @NotNull [] payload);

    /**
     * Subscribe to a topic. The returned {@link AutoCloseable} cancels the
     * subscription when closed. The handler runs on a Redis I/O thread; implementations
     * should not perform blocking work inside it.
     */
    @NotNull AutoCloseable subscribe(@NotNull String topic, @NotNull Consumer<byte[]> handler);

    /**
     * Stable id for this TotemGuard instance, used as the sender id in rollout
     * coordination and as the lock-holder identifier.
     */
    @NotNull UUID instanceId();

    /**
     * Best-effort snapshot of the Redis connection state. Loader code should call this
     * before issuing fleet operations; when {@code false}, callers fall back to the
     * file-only cache path.
     */
    boolean isHealthy();
}
