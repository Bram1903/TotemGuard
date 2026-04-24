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

package com.deathmotion.totemguard.common.cache;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * A thin, string-keyed, bytes-valued cache.
 *
 * <p>Two implementations ship:
 * {@link com.deathmotion.totemguard.common.cache.backend.RedisCacheBackend}
 * and {@link com.deathmotion.totemguard.common.cache.backend.LocalCacheBackend}.
 * The {@link CacheRepositoryImpl} picks exactly one at a time based on
 * Redis connectivity — never writes to both — so there's no cross-backend
 * drift to worry about.</p>
 */
public interface CacheBackend {

    /**
     * @return {@code true} if this backend is currently usable.
     */
    boolean isAvailable();

    /**
     * Reads {@code key} without touching its TTL. Suits data where a fixed
     * cache window matters — history pages, for instance, should refresh
     * from the source on a predictable cadence regardless of how many times
     * they're viewed.
     */
    @Nullable byte[] get(String key);

    /**
     * Reads {@code key} and, on hit, resets its TTL to {@code ttl}. Suits
     * "hot" caches (VPN lookups, staff toggle state, check snapshots) where
     * continued access should keep the entry alive indefinitely.
     */
    @Nullable byte[] getAndRefresh(String key, Duration ttl);

    void put(String key, byte[] value, Duration ttl);

    void remove(String key);

    /**
     * Attempts to install {@code value} at {@code key} iff nothing is there.
     *
     * @return {@code true} when the value was stored, {@code false} if a
     * row already existed (somebody else holds the lock).
     */
    boolean putIfAbsent(String key, byte[] value, Duration ttl);
}
