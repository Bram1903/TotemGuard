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
import com.deathmotion.totemguard.common.cache.CacheBackendException;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// Async commands with a tight per-op timeout so a half-dead connection can't freeze a tick.
public final class RedisCacheBackend implements CacheBackend {

    private static final long OP_TIMEOUT_MS = 250L;

    private final RedisRepositoryImpl repository;

    public RedisCacheBackend(RedisRepositoryImpl repository) {
        this.repository = repository;
    }

    private static <T> @Nullable T await(String op, String key, CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().get(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            stage.toCompletableFuture().cancel(true);
            throw new CacheBackendException(
                    "Redis cache " + op + " for key " + key + " timed out after " + OP_TIMEOUT_MS + "ms", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CacheBackendException("Redis cache " + op + " for key " + key + " was interrupted", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            throw new CacheBackendException(
                    "Redis cache " + op + " failed for key " + key + ": " + cause.getMessage(), cause);
        } catch (Exception ex) {
            throw new CacheBackendException(
                    "Redis cache " + op + " failed for key " + key + ": " + ex.getMessage(), ex);
        }
    }

    private static byte[] bytes(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isAvailable() {
        return repository.isConnected();
    }

    @Override
    public @Nullable byte[] get(String key) {
        return await("get", key, requireCommands().get(bytes(key)));
    }

    @Override
    public @Nullable byte[] getAndRefresh(String key, Duration ttl) {
        long seconds = Math.max(1, ttl.toSeconds());
        return await("getAndRefresh", key, requireCommands().getex(bytes(key), GetExArgs.Builder.ex(seconds)));
    }

    @Override
    public void put(String key, byte[] value, Duration ttl) {
        long seconds = Math.max(1, ttl.toSeconds());
        await("put", key, requireCommands().setex(bytes(key), seconds, value));
    }

    @Override
    public void remove(String key) {
        await("remove", key, requireCommands().del(bytes(key)));
    }

    @Override
    public boolean putIfAbsent(String key, byte[] value, Duration ttl) {
        long seconds = Math.max(1, ttl.toSeconds());
        String response = await("putIfAbsent", key,
                requireCommands().set(bytes(key), value, SetArgs.Builder.nx().ex(seconds)));
        return "OK".equalsIgnoreCase(response);
    }

    private RedisAsyncCommands<byte[], byte[]> requireCommands() {
        RedisConnection conn = repository.connection();
        if (conn == null || !conn.isOpen()) {
            throw new CacheBackendException("Redis cache backend is not available");
        }
        return conn.commands().async();
    }
}
