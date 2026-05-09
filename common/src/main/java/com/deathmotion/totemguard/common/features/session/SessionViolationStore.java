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

package com.deathmotion.totemguard.common.features.session;

import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SessionViolationStore {

    public static final long SESSION_TTL_MS = 5 * 60_000L;
    private static final long PIPELINE_TIMEOUT_SECONDS = 5L;

    private static final String NS = "totemguard:";
    private static final String KEY_SESSION_PREFIX = NS + "session:";
    private static final byte[] KEY_VIOLATORS = bytes(NS + "session:violators");

    private static final byte[] FIELD_NAME = bytes("name");
    private static final byte[] FIELD_SERVER = bytes("srv");
    private static final byte[] FIELD_INSTANCE = bytes("iid");
    private static final byte[] FIELD_SESSION_START = bytes("start");
    private static final byte[] FIELD_LAST_UPDATE = bytes("upd");
    private static final byte[] FIELD_TOTAL = bytes("total");
    private static final String CHECK_FIELD_PREFIX = "c:";

    private static final byte[] PURGE_IF_OWNED_SCRIPT = bytes(
            "local current = redis.call('HGET', KEYS[1], 'iid') "
                    + "if current and current == ARGV[1] then "
                    + "  redis.call('DEL', KEYS[1]) "
                    + "  redis.call('ZREM', KEYS[2], ARGV[2]) "
                    + "  return 1 "
                    + "end "
                    + "return 0");

    private static final byte[] JOIN_SCRIPT = bytes(
            "if redis.call('EXISTS', KEYS[1]) == 1 then "
                    + "  redis.call('HSET', KEYS[1], 'name', ARGV[1], 'srv', ARGV[2], 'iid', ARGV[3], 'upd', ARGV[4]) "
                    + "  redis.call('PEXPIRE', KEYS[1], ARGV[5]) "
                    + "  return 0 "
                    + "end "
                    + "redis.call('HSET', KEYS[1], 'name', ARGV[1], 'srv', ARGV[2], 'iid', ARGV[3], "
                    + "  'start', ARGV[4], 'upd', ARGV[4], 'total', '0') "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[5]) "
                    + "redis.call('ZREM', KEYS[2], ARGV[6]) "
                    + "return 1");

    private final RedisRepositoryImpl redis;
    private final Logger logger;

    public SessionViolationStore(@NotNull RedisRepositoryImpl redis, @NotNull Logger logger) {
        this.redis = redis;
        this.logger = logger;
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] sessionKey(UUID uuid) {
        return bytes(KEY_SESSION_PREFIX + uuid);
    }

    private static byte[] checkField(String checkName) {
        return bytes(CHECK_FIELD_PREFIX + checkName);
    }

    private static @Nullable String decodeString(byte @Nullable [] value) {
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    private static long parseLong(byte @Nullable [] value, long fallback) {
        String str = decodeString(value);
        if (str == null) return fallback;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static int parseInt(byte @Nullable [] value, int fallback) {
        String str = decodeString(value);
        if (str == null) return fallback;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public void onPlayerJoin(@NotNull UUID playerUuid, @NotNull String playerName,
                             @NotNull String serverName, @NotNull UUID instanceId) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return;
        try {
            byte[][] keys = new byte[][]{sessionKey(playerUuid), KEY_VIOLATORS};
            byte[] now = bytes(Long.toString(System.currentTimeMillis()));
            byte[][] argv = new byte[][]{
                    bytes(playerName),
                    bytes(serverName),
                    bytes(instanceId.toString()),
                    now,
                    bytes(Long.toString(SESSION_TTL_MS)),
                    bytes(playerUuid.toString())
            };
            c.eval(JOIN_SCRIPT, ScriptOutputType.INTEGER, keys, argv);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to write session entry for " + playerName, ex);
        }
    }

    public void purgeIfOwnedBy(@NotNull UUID ownerInstanceId, @NotNull Iterable<UUID> playerUuids) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        byte[] iidArg = bytes(ownerInstanceId.toString());
        List<RedisFuture<?>> futures = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            byte[][] keys = new byte[][]{sessionKey(uuid), KEY_VIOLATORS};
            byte[][] argv = new byte[][]{iidArg, bytes(uuid.toString())};
            futures.add(c.eval(PURGE_IF_OWNED_SCRIPT, ScriptOutputType.INTEGER, keys, argv));
        }
        if (!futures.isEmpty()) awaitPipeline("session.purgeIfOwnedBy", futures);
    }

    public void onPlayerQuit(@NotNull UUID playerUuid) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            List<RedisFuture<?>> futures = new ArrayList<>(2);
            futures.add(c.del(sessionKey(playerUuid)));
            futures.add(c.zrem(KEY_VIOLATORS, bytes(playerUuid.toString())));
            awaitPipeline("session.quit", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to clear session entry for " + playerUuid, ex);
        }
    }

    public void recordViolation(@NotNull UUID playerUuid, @NotNull String checkName) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            byte[] key = sessionKey(playerUuid);
            byte[] uuidBytes = bytes(playerUuid.toString());
            long now = System.currentTimeMillis();

            List<RedisFuture<?>> futures = new ArrayList<>(5);
            futures.add(c.hincrby(key, checkField(checkName), 1L));
            futures.add(c.hincrby(key, FIELD_TOTAL, 1L));
            futures.add(c.hset(key, FIELD_LAST_UPDATE, bytes(Long.toString(now))));
            futures.add(c.pexpire(key, SESSION_TTL_MS));
            futures.add(c.zincrby(KEY_VIOLATORS, 1.0d, uuidBytes));

            awaitPipeline("session.violation(" + checkName + ")", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to record violation for " + playerUuid, ex);
        }
    }

    public void heartbeat(@NotNull Iterable<UUID> ownedPlayers) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            List<RedisFuture<?>> futures = new ArrayList<>();
            for (UUID uuid : ownedPlayers) {
                futures.add(c.pexpire(sessionKey(uuid), SESSION_TTL_MS));
            }
            if (!futures.isEmpty()) awaitPipeline("session.heartbeat", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to heartbeat session entries", ex);
        }
    }

    public @Nullable SessionSnapshot getSession(@NotNull UUID playerUuid) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return null;
        try {
            return readSession(c, playerUuid);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "getSession(" + playerUuid + ") failed", ex);
            return null;
        }
    }

    public @NotNull List<SessionSnapshot> topN(int limit) {
        if (limit <= 0) return List.of();
        RedisCommands<byte[], byte[]> sc = sync();
        RedisAsyncCommands<byte[], byte[]> ac = async();
        if (sc == null || ac == null) return List.of();
        try {
            List<ScoredValue<byte[]>> raw = sc.zrevrangebyscoreWithScores(
                    KEY_VIOLATORS,
                    io.lettuce.core.Range.from(io.lettuce.core.Range.Boundary.excluding(0d),
                            io.lettuce.core.Range.Boundary.unbounded()),
                    io.lettuce.core.Limit.create(0, limit));
            if (raw == null || raw.isEmpty()) return List.of();

            List<UUID> uuids = new ArrayList<>(raw.size());
            List<byte[]> uuidBytes = new ArrayList<>(raw.size());
            List<RedisFuture<Map<byte[], byte[]>>> futures = new ArrayList<>(raw.size());
            for (ScoredValue<byte[]> sv : raw) {
                if (!sv.hasValue()) continue;
                UUID uuid;
                try {
                    uuid = UUID.fromString(new String(sv.getValue(), StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                uuids.add(uuid);
                uuidBytes.add(sv.getValue());
                futures.add(ac.hgetall(sessionKey(uuid)));
            }
            if (futures.isEmpty()) return List.of();

            LettuceFutures.awaitAll(PIPELINE_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                    futures.toArray(new RedisFuture<?>[0]));

            List<SessionSnapshot> out = new ArrayList<>(futures.size());
            List<byte[]> stale = null;
            for (int i = 0; i < futures.size(); i++) {
                RedisFuture<Map<byte[], byte[]>> f = futures.get(i);
                Map<byte[], byte[]> hash = null;
                if (f.isDone()) {
                    try {
                        hash = f.get();
                    } catch (Exception ignored) {
                    }
                }
                SessionSnapshot snap = decodeSession(uuids.get(i), hash);
                if (snap == null) {
                    if (stale == null) stale = new ArrayList<>();
                    stale.add(uuidBytes.get(i));
                    continue;
                }
                out.add(snap);
            }
            if (stale != null) {
                try {
                    sc.zrem(KEY_VIOLATORS, stale.toArray(new byte[0][]));
                } catch (Exception ignored) {
                }
            }
            return out;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "topN(" + limit + ") failed", ex);
            return List.of();
        }
    }

    private @Nullable SessionSnapshot readSession(RedisCommands<byte[], byte[]> c, UUID uuid) {
        return decodeSession(uuid, c.hgetall(sessionKey(uuid)));
    }

    private @Nullable SessionSnapshot decodeSession(UUID uuid, @Nullable Map<byte[], byte[]> raw) {
        if (raw == null || raw.isEmpty()) return null;

        Map<String, byte[]> entries = new HashMap<>(raw.size());
        for (Map.Entry<byte[], byte[]> entry : raw.entrySet()) {
            entries.put(new String(entry.getKey(), StandardCharsets.UTF_8), entry.getValue());
        }

        String name = decodeString(entries.get("name"));
        String server = decodeString(entries.get("srv"));
        String instanceStr = decodeString(entries.get("iid"));
        if (name == null || server == null || instanceStr == null) return null;

        UUID instanceId;
        try {
            instanceId = UUID.fromString(instanceStr);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        long now = System.currentTimeMillis();
        long start = parseLong(entries.get("start"), now);
        long upd = parseLong(entries.get("upd"), start);
        int total = parseInt(entries.get("total"), 0);

        Map<String, Integer> violations = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            if (!entry.getKey().startsWith(CHECK_FIELD_PREFIX)) continue;
            int count = parseInt(entry.getValue(), 0);
            if (count <= 0) continue;
            violations.put(entry.getKey().substring(CHECK_FIELD_PREFIX.length()), count);
        }

        return new SessionSnapshot(
                uuid, name, server, instanceId,
                Instant.ofEpochMilli(start),
                Instant.ofEpochMilli(upd),
                total, violations
        );
    }

    private @Nullable RedisCommands<byte[], byte[]> sync() {
        RedisConnection conn = redis.connection();
        if (conn == null || !conn.isOpen()) return null;
        return conn.commands().sync();
    }

    private @Nullable RedisAsyncCommands<byte[], byte[]> async() {
        RedisConnection conn = redis.connection();
        if (conn == null || !conn.isOpen()) return null;
        return conn.commands().async();
    }

    private void awaitPipeline(String op, List<RedisFuture<?>> futures) {
        if (futures.isEmpty()) return;
        boolean ok = LettuceFutures.awaitAll(
                PIPELINE_TIMEOUT_SECONDS, TimeUnit.SECONDS,
                futures.toArray(new RedisFuture<?>[0]));
        if (!ok) logger.log(Level.WARNING, "Redis pipeline " + op + " timed out");
    }
}
