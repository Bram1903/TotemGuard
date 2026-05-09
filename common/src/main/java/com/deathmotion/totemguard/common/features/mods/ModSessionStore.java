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

package com.deathmotion.totemguard.common.features.mods;

import com.deathmotion.totemguard.api.mod.DetectedMod;
import com.deathmotion.totemguard.api.mod.ModDetectionMethod;
import com.deathmotion.totemguard.api.mod.ModSeverity;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ModSessionStore {

    public static final long SESSION_TTL_MS = 5 * 60_000L;
    private static final long PIPELINE_TIMEOUT_SECONDS = 5L;

    private static final String NS = "totemguard:";
    private static final String KEY_MODS_PREFIX = NS + "mods:";

    private static final byte[] FIELD_INSTANCE = bytes("iid");
    private static final byte[] FIELD_SERVER = bytes("srv");
    private static final String MOD_FIELD_PREFIX = "m:";

    private static final byte[] PURGE_IF_OWNED_SCRIPT = bytes(
            "local current = redis.call('HGET', KEYS[1], 'iid') "
                    + "if current and current == ARGV[1] then "
                    + "  redis.call('DEL', KEYS[1]) "
                    + "  return 1 "
                    + "end "
                    + "return 0");

    private final RedisRepositoryImpl redis;
    private final Logger logger;

    public ModSessionStore(@NotNull RedisRepositoryImpl redis, @NotNull Logger logger) {
        this.redis = redis;
        this.logger = logger;
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] modsKey(UUID uuid) {
        return bytes(KEY_MODS_PREFIX + uuid);
    }

    private static byte[] modField(String modId) {
        return bytes(MOD_FIELD_PREFIX + modId);
    }

    private static byte[] encodeMod(DetectedMod mod) {
        return bytes(mod.severity().name() + ":" + mod.detectionMethod().name());
    }

    private static @Nullable DetectedMod decodeMod(String modId, byte @Nullable [] value) {
        if (value == null) return null;
        String raw = new String(value, StandardCharsets.UTF_8);
        int sep = raw.indexOf(':');
        if (sep <= 0 || sep >= raw.length() - 1) return null;
        ModSeverity severity;
        ModDetectionMethod method;
        try {
            severity = ModSeverity.valueOf(raw.substring(0, sep));
            method = ModDetectionMethod.valueOf(raw.substring(sep + 1));
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return new DetectedMod(modId, severity, method);
    }

    public void recordResolved(@NotNull UUID playerUuid,
                               @NotNull UUID instanceId,
                               @NotNull String serverName,
                               @NotNull Set<DetectedMod> mods) {
        if (mods.isEmpty()) return;
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            byte[] key = modsKey(playerUuid);

            Map<byte[], byte[]> fields = new LinkedHashMap<>(mods.size() + 2);
            fields.put(FIELD_INSTANCE, bytes(instanceId.toString()));
            fields.put(FIELD_SERVER, bytes(serverName));
            for (DetectedMod mod : mods) {
                fields.put(modField(mod.id()), encodeMod(mod));
            }

            List<RedisFuture<?>> futures = new ArrayList<>(2);
            futures.add(c.hset(key, fields));
            futures.add(c.pexpire(key, SESSION_TTL_MS));
            awaitPipeline("mods.recordResolved", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to record resolved mods for " + playerUuid, ex);
        }
    }

    public void recordLate(@NotNull UUID playerUuid,
                           @NotNull UUID instanceId,
                           @NotNull String serverName,
                           @NotNull DetectedMod mod) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            byte[] key = modsKey(playerUuid);

            Map<byte[], byte[]> fields = new LinkedHashMap<>(3);
            fields.put(FIELD_INSTANCE, bytes(instanceId.toString()));
            fields.put(FIELD_SERVER, bytes(serverName));
            fields.put(modField(mod.id()), encodeMod(mod));

            List<RedisFuture<?>> futures = new ArrayList<>(2);
            futures.add(c.hset(key, fields));
            futures.add(c.pexpire(key, SESSION_TTL_MS));
            awaitPipeline("mods.recordLate", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to record late mod for " + playerUuid, ex);
        }
    }

    public void onPlayerQuit(@NotNull UUID playerUuid) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            List<RedisFuture<?>> futures = new ArrayList<>(1);
            futures.add(c.del(modsKey(playerUuid)));
            awaitPipeline("mods.quit", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to clear mod entry for " + playerUuid, ex);
        }
    }

    public void heartbeat(@NotNull Iterable<UUID> ownedPlayers) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            List<RedisFuture<?>> futures = new ArrayList<>();
            for (UUID uuid : ownedPlayers) {
                futures.add(c.pexpire(modsKey(uuid), SESSION_TTL_MS));
            }
            if (!futures.isEmpty()) awaitPipeline("mods.heartbeat", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to heartbeat mod entries", ex);
        }
    }

    public void purgeIfOwnedBy(@NotNull UUID ownerInstanceId, @NotNull Iterable<UUID> playerUuids) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        byte[] iidArg = bytes(ownerInstanceId.toString());
        List<RedisFuture<?>> futures = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            byte[][] keys = new byte[][]{modsKey(uuid)};
            byte[][] argv = new byte[][]{iidArg};
            futures.add(c.eval(PURGE_IF_OWNED_SCRIPT, ScriptOutputType.INTEGER, keys, argv));
        }
        if (!futures.isEmpty()) awaitPipeline("mods.purgeIfOwnedBy", futures);
    }

    public @NotNull Set<DetectedMod> getMods(@NotNull UUID playerUuid) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return Set.of();
        try {
            Map<byte[], byte[]> raw = c.hgetall(modsKey(playerUuid));
            if (raw == null || raw.isEmpty()) return Set.of();

            Set<DetectedMod> out = new LinkedHashSet<>();
            for (Map.Entry<byte[], byte[]> entry : raw.entrySet()) {
                String fieldName = new String(entry.getKey(), StandardCharsets.UTF_8);
                if (!fieldName.startsWith(MOD_FIELD_PREFIX)) continue;
                String modId = fieldName.substring(MOD_FIELD_PREFIX.length());
                DetectedMod mod = decodeMod(modId, entry.getValue());
                if (mod != null) out.add(mod);
            }
            return out;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "getMods(" + playerUuid + ") failed", ex);
            return Set.of();
        }
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
