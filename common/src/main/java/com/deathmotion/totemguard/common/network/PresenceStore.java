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

package com.deathmotion.totemguard.common.network;

import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import io.lettuce.core.*;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class PresenceStore {

    public static final long PLAYER_TTL_MS = 60_000L;
    public static final long OWNED_SET_TTL_MS = 600_000L;
    public static final long SERVER_STALE_MS = 30_000L;
    public static final long CLEANUP_LOCK_TTL_MS = 60_000L;
    static final String NS = "totemguard:";
    static final byte[] KEY_PLAYERS = (NS + "players").getBytes(StandardCharsets.UTF_8);
    static final byte[] KEY_NAMES = (NS + "names").getBytes(StandardCharsets.UTF_8);
    static final byte[] KEY_SERVERS = (NS + "servers").getBytes(StandardCharsets.UTF_8);
    static final String KEY_PLAYER_PREFIX = NS + "p:";
    static final String KEY_SERVER_PLAYERS_PREFIX = NS + "s:";
    static final String KEY_CLEANUP_LOCK_PREFIX = NS + "cleanup:";
    private static final long PIPELINE_TIMEOUT_SECONDS = 5L;

    private static final byte[] FIELD_NAME = bytes("name");
    private static final byte[] FIELD_IID = bytes("iid");
    private static final byte[] FIELD_SRV = bytes("srv");
    private static final byte[] FIELD_BYP = bytes("byp");
    private static final byte[] FIELD_TEX_NAME = bytes("tex_name");
    private static final byte[] FIELD_TEX_VALUE = bytes("tex_value");
    private static final byte[] FIELD_TEX_SIG = bytes("tex_sig");
    private static final byte[] BYTE_TRUE = bytes("1");
    private static final byte[] BYTE_FALSE = bytes("0");
    private static final byte DELIMITER = 0;

    private static final byte[] CLAIM_OFFLINE_SCRIPT = bytes(
            "local current = redis.call('HGET', KEYS[1], 'iid') "
                    + "redis.call('SREM', KEYS[4], ARGV[4]) "
                    + "if current and current == ARGV[1] then "
                    + "  redis.call('DEL', KEYS[1]) "
                    + "  redis.call('HDEL', KEYS[2], ARGV[2]) "
                    + "  redis.call('ZREM', KEYS[3], ARGV[3]) "
                    + "  return 1 "
                    + "end "
                    + "return 0");

    private final RedisRepositoryImpl redis;
    private final Logger logger;

    public PresenceStore(RedisRepositoryImpl redis, Logger logger) {
        this.redis = redis;
        this.logger = logger;
    }

    private static void addTextureFields(Map<byte[], byte[]> fields, @Nullable UserProfile profile) {
        if (profile == null) return;
        List<TextureProperty> props = profile.getTextureProperties();
        if (props == null || props.isEmpty()) return;
        TextureProperty first = props.get(0);
        if (first == null) return;
        fields.put(FIELD_TEX_NAME, bytes(first.getName()));
        fields.put(FIELD_TEX_VALUE, bytes(first.getValue()));
        if (first.getSignature() != null) {
            fields.put(FIELD_TEX_SIG, bytes(first.getSignature()));
        }
    }

    private static @Nullable String decodeKv(@Nullable KeyValue<byte[], byte[]> kv) {
        if (kv == null || !kv.hasValue()) return null;
        return new String(kv.getValue(), StandardCharsets.UTF_8);
    }

    private static byte[] playerKey(UUID uuid) {
        return bytes(KEY_PLAYER_PREFIX + uuid);
    }

    private static byte[] serverPlayersKey(UUID instanceId) {
        return bytes(KEY_SERVER_PLAYERS_PREFIX + instanceId + ":p");
    }

    private static byte[] encodeNameMember(String lowername, String displayName) {
        byte[] lb = lowername.getBytes(StandardCharsets.UTF_8);
        byte[] db = displayName.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[lb.length + 1 + db.length];
        System.arraycopy(lb, 0, out, 0, lb.length);
        out[lb.length] = DELIMITER;
        System.arraycopy(db, 0, out, lb.length + 1, db.length);
        return out;
    }

    private static @Nullable String extractDisplay(byte[] member) {
        if (member == null) return null;
        for (int i = 0; i < member.length; i++) {
            if (member[i] == DELIMITER) {
                return new String(member, i + 1, member.length - i - 1, StandardCharsets.UTF_8);
            }
        }
        return new String(member, StandardCharsets.UTF_8);
    }

    private static @Nullable String decodeString(byte @Nullable [] bytes) {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private static long parseLastHeartbeat(byte @Nullable [] value) {
        String str = decodeString(value);
        if (str == null) return 0L;
        int last = str.lastIndexOf('|');
        if (last < 0) return 0L;
        try {
            return Long.parseLong(str.substring(last + 1));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static @Nullable String parseServerName(byte @Nullable [] value) {
        String str = decodeString(value);
        if (str == null) return null;
        int last = str.lastIndexOf('|');
        if (last < 0) return str;
        return str.substring(0, last);
    }

    public void addPlayer(@NotNull UUID playerUuid, @NotNull String name,
                          @NotNull UUID instanceId, @NotNull String serverName,
                          @Nullable UserProfile profile, boolean bypassed) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            String lowername = name.toLowerCase(Locale.ROOT);
            byte[] playerKey = playerKey(playerUuid);
            byte[] uuidBytes = bytes(playerUuid.toString());
            byte[] serverPlayersKey = serverPlayersKey(instanceId);

            Map<byte[], byte[]> playerFields = new HashMap<>();
            playerFields.put(FIELD_NAME, bytes(name));
            playerFields.put(FIELD_IID, bytes(instanceId.toString()));
            playerFields.put(FIELD_SRV, bytes(serverName));
            playerFields.put(FIELD_BYP, bypassed ? BYTE_TRUE : BYTE_FALSE);
            addTextureFields(playerFields, profile);

            List<RedisFuture<?>> futures = new ArrayList<>(6);
            futures.add(c.hset(playerKey, playerFields));
            futures.add(c.pexpire(playerKey, PLAYER_TTL_MS));
            futures.add(c.hset(KEY_PLAYERS, bytes(lowername), uuidBytes));
            futures.add(c.zadd(KEY_NAMES, 0d, encodeNameMember(lowername, name)));
            futures.add(c.sadd(serverPlayersKey, uuidBytes));
            futures.add(c.pexpire(serverPlayersKey, OWNED_SET_TTL_MS));

            awaitPipeline("addPlayer(" + name + ")", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to write presence for " + name, ex);
        }
    }

    public @Nullable UserProfile loadProfile(@NotNull UUID playerUuid) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return null;
        try {
            byte[] playerKey = playerKey(playerUuid);
            List<KeyValue<byte[], byte[]>> values = c.hmget(playerKey,
                    FIELD_NAME, FIELD_TEX_NAME, FIELD_TEX_VALUE, FIELD_TEX_SIG);
            if (values == null || values.size() < 4) return null;
            String name = decodeKv(values.get(0));
            if (name == null) return null;
            UserProfile profile = new UserProfile(playerUuid, name);
            String texName = decodeKv(values.get(1));
            String texValue = decodeKv(values.get(2));
            String texSig = decodeKv(values.get(3));
            if (texName != null && texValue != null) {
                profile.getTextureProperties().add(new TextureProperty(texName, texValue, texSig));
            }
            return profile;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "loadProfile(" + playerUuid + ") failed", ex);
            return null;
        }
    }

    public @Nullable Boolean claimOfflineIfOwned(@NotNull UUID playerUuid, @NotNull String name,
                                                 @NotNull UUID instanceId) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return null;
        try {
            String lowername = name.toLowerCase(Locale.ROOT);
            byte[][] keys = new byte[][]{
                    playerKey(playerUuid),
                    KEY_PLAYERS,
                    KEY_NAMES,
                    serverPlayersKey(instanceId)
            };
            byte[][] argv = new byte[][]{
                    bytes(instanceId.toString()),
                    bytes(lowername),
                    encodeNameMember(lowername, name),
                    bytes(playerUuid.toString())
            };
            Long result = c.eval(CLAIM_OFFLINE_SCRIPT, ScriptOutputType.INTEGER, keys, argv);
            return result != null && result == 1L;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "claimOfflineIfOwned(" + name + ") failed", ex);
            return null;
        }
    }

    public void updateHostServerName(@NotNull UUID instanceId, @NotNull String newName,
                                     @NotNull Iterable<UUID> ownedPlayers) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            byte[] nameBytes = bytes(newName);
            List<RedisFuture<?>> futures = new ArrayList<>();
            for (UUID uuid : ownedPlayers) {
                futures.add(c.hset(playerKey(uuid), FIELD_SRV, nameBytes));
            }
            if (!futures.isEmpty()) awaitPipeline("updateHostServerName", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to update host server name to " + newName, ex);
        }
    }

    public void heartbeatHost(@NotNull UUID instanceId, @NotNull String serverName,
                              @NotNull Iterable<UUID> ownedPlayers) {
        RedisAsyncCommands<byte[], byte[]> c = async();
        if (c == null) return;
        try {
            long now = System.currentTimeMillis();
            byte[] entry = bytes(serverName + "|" + now);
            byte[] serverPlayersKey = serverPlayersKey(instanceId);

            List<RedisFuture<?>> futures = new ArrayList<>();
            futures.add(c.hset(KEY_SERVERS, bytes(instanceId.toString()), entry));
            futures.add(c.pexpire(serverPlayersKey, OWNED_SET_TTL_MS));
            for (UUID uuid : ownedPlayers) {
                futures.add(c.pexpire(playerKey(uuid), PLAYER_TTL_MS));
            }
            awaitPipeline("heartbeatHost", futures);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to heartbeat host presence", ex);
        }
    }

    public @NotNull List<RemotePlayerEntry> purgeServer(@NotNull UUID instanceId, @NotNull String serverName) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return List.of();
        try {
            List<RemotePlayerEntry> orphaned = new ArrayList<>();
            byte[] serverPlayersKey = serverPlayersKey(instanceId);
            byte[] iidArg = bytes(instanceId.toString());

            for (byte[] uuidBytes : c.smembers(serverPlayersKey)) {
                UUID playerUuid;
                try {
                    playerUuid = UUID.fromString(new String(uuidBytes, StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                byte[] playerKey = playerKey(playerUuid);
                String name = decodeString(c.hget(playerKey, FIELD_NAME));
                if (name == null) continue;

                String lowername = name.toLowerCase(Locale.ROOT);
                byte[][] keys = new byte[][]{playerKey, KEY_PLAYERS, KEY_NAMES, serverPlayersKey};
                byte[][] argv = new byte[][]{
                        iidArg,
                        bytes(lowername),
                        encodeNameMember(lowername, name),
                        uuidBytes
                };
                Long result = c.eval(CLAIM_OFFLINE_SCRIPT, ScriptOutputType.INTEGER, keys, argv);
                if (result != null && result == 1L) {
                    orphaned.add(new RemotePlayerEntry(playerUuid, name, instanceId, serverName, false));
                }
            }
            c.del(serverPlayersKey);
            c.hdel(KEY_SERVERS, bytes(instanceId.toString()));
            return orphaned;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to purge server " + instanceId, ex);
            return List.of();
        }
    }

    public @NotNull Map<UUID, List<RemotePlayerEntry>> sweepStaleServers(@NotNull UUID ourInstanceId) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return Map.of();
        try {
            Map<byte[], byte[]> servers = c.hgetall(KEY_SERVERS);
            if (servers == null || servers.isEmpty()) return Map.of();
            long cutoff = System.currentTimeMillis() - SERVER_STALE_MS;

            Map<UUID, List<RemotePlayerEntry>> purged = new LinkedHashMap<>();
            for (Map.Entry<byte[], byte[]> entry : servers.entrySet()) {
                UUID iid;
                try {
                    iid = UUID.fromString(new String(entry.getKey(), StandardCharsets.UTF_8));
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (iid.equals(ourInstanceId)) continue;
                long lastHb = parseLastHeartbeat(entry.getValue());
                if (lastHb >= cutoff) continue;
                String serverName = parseServerName(entry.getValue());

                byte[] lockKey = bytes(KEY_CLEANUP_LOCK_PREFIX + iid);
                String acquired = c.set(lockKey, bytes(ourInstanceId.toString()),
                        io.lettuce.core.SetArgs.Builder.nx().px(CLEANUP_LOCK_TTL_MS));
                if (acquired == null) continue;
                try {
                    purged.put(iid, purgeServer(iid, serverName == null ? "" : serverName));
                } finally {
                    c.del(lockKey);
                }
            }
            return purged;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to sweep stale servers", ex);
            return Map.of();
        }
    }

    public @Nullable RemotePlayerEntry findByUuid(@NotNull UUID playerUuid) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return null;
        try {
            return readPlayer(c, playerUuid);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "findByUuid(" + playerUuid + ") failed", ex);
            return null;
        }
    }

    public @Nullable RemotePlayerEntry findByName(@NotNull String name) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return null;
        try {
            byte[] uuidBytes = c.hget(KEY_PLAYERS, bytes(name.toLowerCase(Locale.ROOT)));
            if (uuidBytes == null) return null;
            UUID uuid = UUID.fromString(new String(uuidBytes, StandardCharsets.UTF_8));
            return readPlayer(c, uuid);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "findByName(" + name + ") failed", ex);
            return null;
        }
    }

    public @NotNull List<String> suggestNames(@NotNull String prefix, int limit) {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return List.of();
        try {
            String lower = prefix.toLowerCase(Locale.ROOT);
            Range<byte[]> range;
            if (lower.isEmpty()) {
                range = Range.unbounded();
            } else {
                byte[] start = bytes(lower);
                byte[] end = new byte[start.length + 1];
                System.arraycopy(start, 0, end, 0, start.length);
                end[start.length] = (byte) 0xff;
                range = Range.from(Range.Boundary.including(start), Range.Boundary.including(end));
            }
            List<byte[]> raw = c.zrangebylex(KEY_NAMES, range, Limit.create(0, limit));
            if (raw == null || raw.isEmpty()) return List.of();
            List<String> result = new ArrayList<>(raw.size());
            for (byte[] member : raw) {
                String display = extractDisplay(member);
                if (display != null) result.add(display);
            }
            return result;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "suggestNames(" + prefix + ") failed", ex);
            return List.of();
        }
    }

    public int trackedPlayerCount() {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return 0;
        try {
            Long len = c.hlen(KEY_PLAYERS);
            return len == null ? 0 : len.intValue();
        } catch (Exception ex) {
            return 0;
        }
    }

    /**
     * Scans {@link #KEY_PLAYERS} and removes any entry whose backing
     * {@code totemguard:p:<uuid>} key no longer exists. The player hash and the
     * name zset have no TTL of their own, so they only get pruned by
     * {@link #claimOfflineIfOwned} (clean quit) or {@link #purgeServer} (sweep).
     * If a server dies hard and stays offline past {@link #OWNED_SET_TTL_MS}, its
     * {@code s:<iid>:p} ownership index expires and {@code purgeServer} can no
     * longer find the players to delete, leaving orphans that would otherwise
     * inflate {@link #trackedPlayerCount()} forever.
     */
    public int reconcileOrphanPlayers() {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return 0;
        int removed = 0;
        try {
            ScanArgs args = ScanArgs.Builder.limit(200);
            MapScanCursor<byte[], byte[]> cursor = c.hscan(KEY_PLAYERS, ScanCursor.INITIAL, args);
            while (true) {
                for (Map.Entry<byte[], byte[]> entry : cursor.getMap().entrySet()) {
                    if (reconcileOnePlayer(c, entry.getKey(), entry.getValue())) removed++;
                }
                if (cursor.isFinished()) break;
                cursor = c.hscan(KEY_PLAYERS, cursor, args);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to reconcile orphan players", ex);
        }
        return removed;
    }

    private boolean reconcileOnePlayer(RedisCommands<byte[], byte[]> c, byte[] lowernameKey, byte[] uuidValue) {
        String lowername = new String(lowernameKey, StandardCharsets.UTF_8);
        UUID uuid;
        try {
            uuid = UUID.fromString(new String(uuidValue, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException ex) {
            c.hdel(KEY_PLAYERS, lowernameKey);
            removeNameZsetEntries(c, lowername);
            return true;
        }
        Long exists = c.exists(playerKey(uuid));
        if (exists != null && exists > 0L) return false;
        c.hdel(KEY_PLAYERS, lowernameKey);
        removeNameZsetEntries(c, lowername);
        return true;
    }

    private void removeNameZsetEntries(RedisCommands<byte[], byte[]> c, String lowername) {
        byte[] lowerBytes = bytes(lowername);
        byte[] start = new byte[lowerBytes.length + 1];
        System.arraycopy(lowerBytes, 0, start, 0, lowerBytes.length);
        start[lowerBytes.length] = DELIMITER;
        byte[] end = new byte[lowerBytes.length + 1];
        System.arraycopy(lowerBytes, 0, end, 0, lowerBytes.length);
        end[lowerBytes.length] = (byte) (DELIMITER + 1);
        Range<byte[]> range = Range.from(Range.Boundary.including(start), Range.Boundary.excluding(end));
        List<byte[]> members = c.zrangebylex(KEY_NAMES, range, Limit.unlimited());
        if (members == null || members.isEmpty()) return;
        c.zrem(KEY_NAMES, members.toArray(new byte[0][]));
    }

    public int countServers() {
        RedisCommands<byte[], byte[]> c = sync();
        if (c == null) return 0;
        try {
            Map<byte[], byte[]> servers = c.hgetall(KEY_SERVERS);
            if (servers == null || servers.isEmpty()) return 0;
            long cutoff = System.currentTimeMillis() - SERVER_STALE_MS;
            int count = 0;
            for (Map.Entry<byte[], byte[]> entry : servers.entrySet()) {
                if (parseLastHeartbeat(entry.getValue()) >= cutoff) count++;
            }
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }

    private @Nullable RemotePlayerEntry readPlayer(RedisCommands<byte[], byte[]> c, UUID uuid) {
        List<KeyValue<byte[], byte[]>> values = c.hmget(playerKey(uuid), FIELD_NAME, FIELD_IID, FIELD_SRV, FIELD_BYP);
        if (values == null || values.size() < 3) return null;
        String name = decodeKv(values.get(0));
        String iidStr = decodeKv(values.get(1));
        String srv = decodeKv(values.get(2));
        if (name == null || iidStr == null || srv == null) return null;
        UUID iid;
        try {
            iid = UUID.fromString(iidStr);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        String bypStr = values.size() >= 4 ? decodeKv(values.get(3)) : null;
        boolean bypassed = "1".equals(bypStr);
        return new RemotePlayerEntry(uuid, name, iid, srv, bypassed);
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
