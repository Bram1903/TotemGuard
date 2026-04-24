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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheBackend;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Level;

public final class RedisCacheBackend implements CacheBackend {

    private final RedisRepositoryImpl redisRepository;

    public RedisCacheBackend(RedisRepositoryImpl redisRepository) {
        this.redisRepository = redisRepository;
    }

    private static byte[] bytes(String key) {
        return key.getBytes(StandardCharsets.UTF_8);
    }

    private static void logWarn(String op, String key, Throwable t) {
        TGPlatform.getInstance().getLogger().log(Level.WARNING,
                "Redis cache " + op + " failed for key " + key + ": " + t.getMessage());
    }

    @Override
    public boolean isAvailable() {
        return redisRepository.isConnected();
    }

    @Override
    public @Nullable byte[] get(String key) {
        RedisCommands<byte[], byte[]> sync = sync();
        if (sync == null) return null;
        try {
            return sync.get(bytes(key));
        } catch (Exception ex) {
            logWarn("get", key, ex);
            return null;
        }
    }

    @Override
    public @Nullable byte[] getAndRefresh(String key, Duration ttl) {
        RedisCommands<byte[], byte[]> sync = sync();
        if (sync == null) return null;
        try {
            long seconds = Math.max(1, ttl.toSeconds());
            return sync.getex(bytes(key), GetExArgs.Builder.ex(seconds));
        } catch (Exception ex) {
            logWarn("getAndRefresh", key, ex);
            return null;
        }
    }

    @Override
    public void put(String key, byte[] value, Duration ttl) {
        RedisCommands<byte[], byte[]> sync = sync();
        if (sync == null) return;
        try {
            long seconds = Math.max(1, ttl.toSeconds());
            sync.setex(bytes(key), seconds, value);
        } catch (Exception ex) {
            logWarn("put", key, ex);
        }
    }

    @Override
    public void remove(String key) {
        RedisCommands<byte[], byte[]> sync = sync();
        if (sync == null) return;
        try {
            sync.del(bytes(key));
        } catch (Exception ex) {
            logWarn("remove", key, ex);
        }
    }

    @Override
    public boolean putIfAbsent(String key, byte[] value, Duration ttl) {
        RedisCommands<byte[], byte[]> sync = sync();
        if (sync == null) return false;
        try {
            long seconds = Math.max(1, ttl.toSeconds());
            String response = sync.set(bytes(key), value, SetArgs.Builder.nx().ex(seconds));
            return "OK".equalsIgnoreCase(response);
        } catch (Exception ex) {
            logWarn("putIfAbsent", key, ex);
            return false;
        }
    }

    private @Nullable RedisCommands<byte[], byte[]> sync() {
        if (!redisRepository.isConnected()) return null;
        return redisRepository.sync();
    }
}
