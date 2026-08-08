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

package com.deathmotion.totemguard.common.features.follow;

import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FollowStore {

    static final byte[] KEY = "totemguard:follow:state".getBytes(StandardCharsets.UTF_8);
    private static final byte DELIMITER = 0;

    private final RedisRepositoryImpl redis;
    private final Logger logger;

    public FollowStore(@NotNull RedisRepositoryImpl redis, @NotNull Logger logger) {
        this.redis = redis;
        this.logger = logger;
    }

    private static byte[] field(UUID followerUuid) {
        return followerUuid.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] encode(@NotNull FollowState state) {
        byte[] tu = state.targetUuid().toString().getBytes(StandardCharsets.UTF_8);
        byte[] tn = state.targetName().getBytes(StandardCharsets.UTF_8);
        byte[] ti = state.targetServerInstance().toString().getBytes(StandardCharsets.UTF_8);
        byte[] ts = state.targetServerName().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[tu.length + tn.length + ti.length + ts.length + 3];
        int p = 0;
        System.arraycopy(tu, 0, out, p, tu.length);
        p += tu.length;
        out[p++] = DELIMITER;
        System.arraycopy(tn, 0, out, p, tn.length);
        p += tn.length;
        out[p++] = DELIMITER;
        System.arraycopy(ti, 0, out, p, ti.length);
        p += ti.length;
        out[p++] = DELIMITER;
        System.arraycopy(ts, 0, out, p, ts.length);
        return out;
    }

    private static @Nullable FollowState decode(@NotNull UUID followerUuid, byte[] raw) {
        if (raw == null || raw.length == 0) return null;
        int[] sep = new int[3];
        int found = 0;
        for (int i = 0; i < raw.length && found < 3; i++) {
            if (raw[i] == DELIMITER) sep[found++] = i;
        }
        if (found != 3) return null;
        try {
            UUID targetUuid = UUID.fromString(new String(raw, 0, sep[0], StandardCharsets.UTF_8));
            String targetName = new String(raw, sep[0] + 1, sep[1] - sep[0] - 1, StandardCharsets.UTF_8);
            UUID targetInstance = UUID.fromString(
                    new String(raw, sep[1] + 1, sep[2] - sep[1] - 1, StandardCharsets.UTF_8));
            String targetServerName = new String(
                    raw, sep[2] + 1, raw.length - sep[2] - 1, StandardCharsets.UTF_8);
            return new FollowState(followerUuid, targetUuid, targetName, targetInstance, targetServerName);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public @Nullable FollowState load(@NotNull UUID followerUuid) {
        RedisCommands<byte[], byte[]> sync = sync();
        if (sync == null) return null;
        try {
            byte[] raw = sync.hget(KEY, field(followerUuid));
            if (raw == null) return null;
            return decode(followerUuid, raw);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "FollowStore load failed: " + ex.getMessage());
            return null;
        }
    }

    public @NotNull Map<UUID, FollowState> loadAll() {
        RedisCommands<byte[], byte[]> sync = sync();
        if (sync == null) return Map.of();
        try {
            Map<byte[], byte[]> all = sync.hgetall(KEY);
            if (all == null || all.isEmpty()) return Map.of();
            Map<UUID, FollowState> out = new HashMap<>(all.size());
            for (Map.Entry<byte[], byte[]> entry : all.entrySet()) {
                try {
                    UUID follower = UUID.fromString(new String(entry.getKey(), StandardCharsets.UTF_8));
                    FollowState state = decode(follower, entry.getValue());
                    if (state != null) out.put(follower, state);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return out;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "FollowStore loadAll failed: " + ex.getMessage());
            return Map.of();
        }
    }

    public void save(@NotNull FollowState state) {
        RedisAsyncCommands<byte[], byte[]> async = async();
        if (async == null) return;
        try {
            async.hset(KEY, field(state.followerUuid()), encode(state));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "FollowStore save failed: " + ex.getMessage());
        }
    }

    public void remove(@NotNull UUID followerUuid) {
        RedisAsyncCommands<byte[], byte[]> async = async();
        if (async == null) return;
        try {
            async.hdel(KEY, field(followerUuid));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "FollowStore remove failed: " + ex.getMessage());
        }
    }

    private @Nullable RedisCommands<byte[], byte[]> sync() {
        if (!redis.isClusterMode()) return null;
        RedisConnection conn = redis.connection();
        if (conn == null || !conn.isOpen()) return null;
        return conn.commands().sync();
    }

    private @Nullable RedisAsyncCommands<byte[], byte[]> async() {
        if (!redis.isClusterMode()) return null;
        RedisConnection conn = redis.connection();
        if (conn == null || !conn.isOpen()) return null;
        return conn.commands().async();
    }
}
