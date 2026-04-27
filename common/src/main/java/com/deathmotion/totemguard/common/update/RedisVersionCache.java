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

package com.deathmotion.totemguard.common.update;

import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

final class RedisVersionCache {

    private static final long OP_TIMEOUT_MS = 250L;

    private final RedisRepositoryImpl repository;
    private final byte[] keyBytes;
    private final Duration ttl;

    RedisVersionCache(RedisRepositoryImpl repository, String key, Duration ttl) {
        this.repository = repository;
        this.keyBytes = key.getBytes(StandardCharsets.UTF_8);
        this.ttl = ttl;
    }

    @Nullable String read() {
        RedisConnection conn = repository.connection();
        if (conn == null || !conn.isOpen()) return null;
        try {
            byte[] raw = conn.commands().async().get(keyBytes).toCompletableFuture().get(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return raw == null ? null : new String(raw, StandardCharsets.UTF_8);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    void write(String value) {
        RedisConnection conn = repository.connection();
        if (conn == null || !conn.isOpen()) return;
        try {
            conn.commands().async().setex(keyBytes, Math.max(1, ttl.toSeconds()), value.getBytes(StandardCharsets.UTF_8))
                    .toCompletableFuture().get(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }

    void invalidate() {
        RedisConnection conn = repository.connection();
        if (conn == null || !conn.isOpen()) return;
        try {
            conn.commands().async().del(keyBytes).toCompletableFuture().get(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        }
    }
}
