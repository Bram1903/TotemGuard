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

import com.deathmotion.totemguard.api3.config.Config;
import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.api3.config.key.impl.ConfigKeys;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.backend.LocalCacheBackend;
import com.deathmotion.totemguard.common.cache.backend.RedisCacheBackend;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.logging.Level;

/**
 * Unified cache facade. Every call routes to exactly one backend:
 * Redis when it's available, the in-process store otherwise. This plugin
 * never double-writes, so the two backends cannot drift.
 *
 * <p>The repository is intentionally value-type-agnostic — callers supply a
 * {@link Codec} at every call. That keeps all wire formats in one place
 * ({@link CacheCodecs}) instead of scattered across data records, and it
 * makes swapping backends or adding new types a pure additive change.</p>
 */
public final class CacheRepositoryImpl {

    private final RedisCacheBackend redisBackend;
    private final LocalCacheBackend localBackend;

    private volatile boolean enabled;

    public CacheRepositoryImpl() {
        TGPlatform platform = TGPlatform.getInstance();
        this.redisBackend = new RedisCacheBackend(platform.getRedisRepository());
        this.localBackend = new LocalCacheBackend();
        reload();
    }

    /**
     * Reloads the enabled flag from config. No TTLs live here — each caller owns its own.
     */
    public void reload() {
        Config config = TGPlatform.getInstance().getConfigRepository().config(ConfigFile.CONFIG);
        this.enabled = config.getBoolean(ConfigKeys.CACHE_ENABLED);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return whether the currently-selected backend speaks Redis.
     */
    public boolean isDistributed() {
        return redisBackend.isAvailable();
    }

    /**
     * Fetches the cached value at {@code key} and decodes it through
     * {@code codec}, returning {@code null} on any miss, decode failure, or
     * when caching is disabled.
     *
     * <p>The TTL is not touched — use {@link #getAndRefresh} when you want
     * "as long as someone keeps looking at this, keep it warm" semantics.
     * Plain {@code get} gives you "this entry dies on its original schedule
     * no matter how many reads happen" — right for history pages that must
     * refresh on a predictable cadence.</p>
     */
    public <V> @Nullable V get(String key, Codec<V> codec) {
        if (!enabled) return null;
        return decode(key, backend().get(key), codec);
    }

    /**
     * Like {@link #get} but, on hit, resets the entry's TTL to {@code ttl}
     * so continued use keeps the cached copy alive. Use for hot lookups
     * like VPN results and staff toggle state — things a DB query is
     * expensive to re-materialize.
     */
    public <V> @Nullable V getAndRefresh(String key, Codec<V> codec, Duration ttl) {
        if (!enabled) return null;
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

    /**
     * Stores {@code value} under {@code key} for {@code ttl}. Silently no-ops if caching is off.
     */
    public <V> void put(String key, V value, Codec<V> codec, Duration ttl) {
        if (!enabled) return;
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
     * Lock-style write — only installs the value if no row exists.
     *
     * @return {@code true} iff this caller claimed the slot
     */
    public <V> boolean putIfAbsent(String key, V value, Codec<V> codec, Duration ttl) {
        if (!enabled) return true;
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
        if (!enabled) return;
        backend().remove(key);
    }

    /**
     * Checks presence without decoding. Cheap for key-only questions like
     * "is there a lock for this uuid".
     */
    public boolean contains(String key) {
        if (!enabled) return false;
        return backend().get(key) != null;
    }

    private CacheBackend backend() {
        return redisBackend.isAvailable() ? redisBackend : localBackend;
    }
}
