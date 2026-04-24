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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.backend.LocalCacheBackend;
import com.deathmotion.totemguard.common.cache.backend.RedisCacheBackend;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.logging.Level;

/**
 * Routes every call to either Redis or the in-process backend — never both.
 */
public final class CacheRepositoryImpl {

    private final RedisCacheBackend redisBackend;
    private final LocalCacheBackend localBackend;

    public CacheRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();
        this.redisBackend = new RedisCacheBackend(platform.getRedisRepository());
        this.localBackend = new LocalCacheBackend();
    }

    public boolean isDistributed() {
        return redisBackend.isAvailable();
    }

    /**
     * Reads without touching the TTL.
     */
    public <V> @Nullable V get(String key, Codec<V> codec) {
        return decode(key, backend().get(key), codec);
    }

    /**
     * Reads and, on hit, resets the TTL to {@code ttl}.
     */
    public <V> @Nullable V getAndRefresh(String key, Codec<V> codec, Duration ttl) {
        return decode(key, backend().getAndRefresh(key, ttl), codec);
    }

    private <V> @Nullable V decode(String key, byte[] raw, Codec<V> codec) {
        if (raw == null) return null;
        try {
            return codec.decode(raw);
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Cache decode failed for key " + key, ex);
            backend().remove(key);
            return null;
        }
    }

    public <V> void put(String key, V value, Codec<V> codec, Duration ttl) {
        byte[] raw;
        try {
            raw = codec.encode(value);
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Cache encode failed for key " + key, ex);
            return;
        }
        backend().put(key, raw, ttl);
    }

    /**
     * @return {@code true} iff this caller claimed the slot.
     */
    public <V> boolean putIfAbsent(String key, V value, Codec<V> codec, Duration ttl) {
        byte[] raw;
        try {
            raw = codec.encode(value);
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Cache encode failed for key " + key, ex);
            return false;
        }
        return backend().putIfAbsent(key, raw, ttl);
    }

    public void remove(String key) {
        backend().remove(key);
    }

    public boolean contains(String key) {
        return backend().get(key) != null;
    }

    private CacheBackend backend() {
        return redisBackend.isAvailable() ? redisBackend : localBackend;
    }
}
