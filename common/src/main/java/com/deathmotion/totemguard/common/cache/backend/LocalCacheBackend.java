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
 * Guava cache with per-entry TTLs — Guava only supports one TTL per cache,
 * so we stamp each entry with its own expiry and check at read time.
 */
public final class LocalCacheBackend implements CacheBackend {

    private static final int MAX_ENTRIES = 10_000;

    private final Cache<String, Entry> cache;
    private final ConcurrentMap<String, Entry> view;

    public LocalCacheBackend() {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(MAX_ENTRIES)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
        this.view = cache.asMap();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public byte @Nullable [] get(String key) {
        Entry entry = view.get(key);
        if (entry == null) return null;
        if (entry.expired()) {
            view.computeIfPresent(key, (k, current) -> current.expired() ? null : current);
            return null;
        }
        return entry.value();
    }

    @Override
    public byte @Nullable [] getAndRefresh(String key, Duration ttl) {
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
        }
    }

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
