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
import com.deathmotion.totemguard.common.util.TGVersions;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

// Redis primary, local write-through mirror so reads survive transient Redis outages.
public final class CacheRepositoryImpl {

    private static final long FAILURE_LOG_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();
    private static final Codec<byte[]> RAW_CODEC = new Codec<>() {
        @Override
        public byte[] encode(byte[] value) {
            return value;
        }

        @Override
        public byte[] decode(byte[] raw) {
            return raw;
        }
    };
    private final RedisCacheBackend redisBackend;
    private final LocalCacheBackend localBackend;
    private final AtomicLong lastFailureLogNanos = new AtomicLong(Long.MIN_VALUE);

    public CacheRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();
        this.redisBackend = new RedisCacheBackend(platform.getRedisRepository(), TGVersions.CURRENT);
        this.localBackend = new LocalCacheBackend();
    }

    public boolean isDistributed() {
        return redisBackend.isAvailable();
    }

    public <V> @Nullable V get(String key, Codec<V> codec) {
        return decode(key, readBytes(key, false, Duration.ZERO), codec);
    }

    public <V> @Nullable V getAndRefresh(String key, Codec<V> codec, Duration ttl) {
        return decode(key, readBytes(key, true, ttl), codec);
    }

    public <V> void put(String key, V value, Codec<V> codec, Duration ttl) {
        byte[] raw = encode(key, value, codec);
        if (raw == null) return;

        localBackend.put(key, raw, ttl);

        if (!redisBackend.isAvailable()) return;
        try {
            redisBackend.put(key, raw, ttl);
        } catch (CacheBackendException ex) {
            logFallback("put", key, ex);
        }
    }

    public <V> boolean putIfAbsent(String key, V value, Codec<V> codec, Duration ttl) {
        byte[] raw = encode(key, value, codec);
        if (raw == null) return false;

        if (!redisBackend.isAvailable()) {
            return localBackend.putIfAbsent(key, raw, ttl);
        }
        try {
            boolean claimed = redisBackend.putIfAbsent(key, raw, ttl);
            if (claimed) localBackend.put(key, raw, ttl);
            return claimed;
        } catch (CacheBackendException ex) {
            logFallback("putIfAbsent", key, ex);
            return localBackend.putIfAbsent(key, raw, ttl);
        }
    }

    public void remove(String key) {
        localBackend.remove(key);
        if (!redisBackend.isAvailable()) return;
        try {
            redisBackend.remove(key);
        } catch (CacheBackendException ex) {
            logFallback("remove", key, ex);
        }
    }

    public boolean contains(String key) {
        return get(key, RAW_CODEC) != null;
    }

    private byte @Nullable [] readBytes(String key, boolean refresh, Duration ttl) {
        if (redisBackend.isAvailable()) {
            try {
                byte[] raw = refresh ? redisBackend.getAndRefresh(key, ttl) : redisBackend.get(key);
                if (raw != null && refresh) {
                    localBackend.put(key, raw, ttl);
                }
                return raw;
            } catch (CacheBackendException ex) {
                logFallback(refresh ? "getAndRefresh" : "get", key, ex);
            }
        }
        return refresh ? localBackend.getAndRefresh(key, ttl) : localBackend.get(key);
    }

    private <V> byte[] encode(String key, V value, Codec<V> codec) {
        try {
            return codec.encode(value);
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Cache encode failed for key " + key, ex);
            return null;
        }
    }

    private <V> @Nullable V decode(String key, byte[] raw, Codec<V> codec) {
        if (raw == null) return null;
        try {
            return codec.decode(raw);
        } catch (Exception ex) {
            TGPlatform.getInstance().getLogger().log(Level.WARNING,
                    "Cache decode failed for key " + key, ex);
            try {
                if (redisBackend.isAvailable()) redisBackend.remove(key);
            } catch (CacheBackendException ignored) {
            }
            localBackend.remove(key);
            return null;
        }
    }

    private void logFallback(String op, String key, CacheBackendException ex) {
        long now = System.nanoTime();
        long last = lastFailureLogNanos.get();
        if (last != Long.MIN_VALUE && now - last < FAILURE_LOG_INTERVAL_NANOS) {
            return;
        }
        if (!lastFailureLogNanos.compareAndSet(last, now)) return;
        TGPlatform.getInstance().getLogger().log(Level.WARNING,
                "Redis cache " + op + " for key " + key
                        + " failed — falling back to local cache: " + ex.getMessage());
    }
}
