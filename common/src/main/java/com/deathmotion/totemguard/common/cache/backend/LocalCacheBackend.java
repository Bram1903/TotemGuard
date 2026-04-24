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

package com.deathmotion.totemguard.common.cache.backend;

import com.deathmotion.totemguard.common.cache.CacheBackend;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * In-process cache backed by a single Guava {@link Cache} plus a ConcurrentHashMap
 * for per-entry expiration.
 *
 * <p>Guava's {@link CacheBuilder} only supports a uniform TTL per cache, but
 * callers bring their own TTL per entry. We store the computed expiry timestamp
 * alongside the bytes and evict at read time — Guava still enforces the
 * bounded-size eviction so memory stays capped.</p>
 */
public final class LocalCacheBackend implements CacheBackend {

    private static final int DEFAULT_MAX_ENTRIES = 10_000;

    private final Cache<String, Entry> cache;
    // Tracks keys that are "reserved" for putIfAbsent correctness. Guava's
    // asMap().putIfAbsent is atomic enough for our single-JVM semantics.
    private final ConcurrentMap<String, Entry> view;

    public LocalCacheBackend() {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(DEFAULT_MAX_ENTRIES)
                // Longest TTL we use is the staff alerts toggle at 30 min, so
                // a 1-hour hard cap keeps dangling entries out of memory even
                // if the per-entry expiry check somehow misses.
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
        this.view = cache.asMap();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public @Nullable byte[] get(String key) {
        Entry entry = view.get(key);
        if (entry == null) return null;
        if (entry.expired()) {
            // computeIfPresent avoids clobbering a racing put
            view.computeIfPresent(key, (k, current) -> current.expired() ? null : current);
            return null;
        }
        return entry.value();
    }

    @Override
    public @Nullable byte[] getAndRefresh(String key, Duration ttl) {
        // computeIfPresent lets us read the current value and swap in a
        // freshly-stamped entry atomically, so concurrent refreshes can't
        // race each other or a put.
        byte[][] out = new byte[1][];
        view.computeIfPresent(key, (k, current) -> {
            if (current.expired()) return null;
            out[0] = current.value();
            return Entry.of(current.value(), ttl);
        });
        return out[0];
    }

    @Override
    public void put(String key, byte[] value, Duration ttl) {
        view.put(key, Entry.of(value, ttl));
    }

    @Override
    public void remove(String key) {
        view.remove(key);
    }

    @Override
    public boolean putIfAbsent(String key, byte[] value, Duration ttl) {
        while (true) {
            Entry current = view.get(key);
            if (current != null && !current.expired()) {
                return false;
            }
            Entry proposed = Entry.of(value, ttl);
            if (current == null) {
                if (view.putIfAbsent(key, proposed) == null) return true;
            } else if (view.replace(key, current, proposed)) {
                return true;
            }
            // another thread raced us — loop and re-check
        }
    }

    /**
     * Used by tests and reload to start from a clean slate.
     */
    public void clear() {
        cache.invalidateAll();
    }

    private static final class Entry {
        private final byte[] value;
        private final long expiresAtNanos;

        private Entry(byte[] value, long expiresAtNanos) {
            this.value = value;
            this.expiresAtNanos = expiresAtNanos;
        }

        static Entry of(byte[] value, Duration ttl) {
            long nanos = ttl.isZero() || ttl.isNegative()
                    ? Long.MAX_VALUE
                    : System.nanoTime() + ttl.toNanos();
            return new Entry(value, nanos);
        }

        byte[] value() {
            return value;
        }

        boolean expired() {
            return expiresAtNanos != Long.MAX_VALUE && System.nanoTime() >= expiresAtNanos;
        }
    }

}
