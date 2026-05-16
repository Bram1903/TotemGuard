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

package com.deathmotion.totemguard.common.fleet;

import com.deathmotion.totemguard.api.fleet.FleetCache;
import com.deathmotion.totemguard.api.fleet.FleetLock;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter that exposes the TotemGuard Redis connection to the loader as a
 * {@link FleetCache}. Lives on the TG side because the loader has no Redis client of
 * its own; TG attaches an instance once it's connected, detaches on disconnect.
 *
 * <p>All ops are best-effort. Transport failures are logged at FINE and swallowed so a
 * misbehaving Redis can't cascade into the loader's hot paths.</p>
 */
public final class RedisFleetCache implements FleetCache {

    private final RedisRepositoryImpl redis;
    private final UUID instanceId;
    private final Logger logger;
    private final Map<String, SubscriberSet> subscriptions = new ConcurrentHashMap<>();
    private final Object pubSubLock = new Object();

    private volatile boolean pubSubAttached;

    public RedisFleetCache(RedisRepositoryImpl redis, UUID instanceId, Logger logger) {
        this.redis = redis;
        this.instanceId = instanceId;
        this.logger = logger;
    }

    @Override
    public @NotNull UUID instanceId() {
        return instanceId;
    }

    @Override
    public boolean isHealthy() {
        return redis.isConnected();
    }

    @Override
    public @NotNull Optional<byte[]> get(@NotNull String key) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return Optional.empty();
        try {
            byte[] value = cmd.get(key.getBytes(StandardCharsets.UTF_8));
            return Optional.ofNullable(value);
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache GET failed for " + key, t);
            return Optional.empty();
        }
    }

    @Override
    public void put(@NotNull String key, byte @NotNull [] value, @NotNull Duration ttl) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return;
        try {
            cmd.set(key.getBytes(StandardCharsets.UTF_8), value, SetArgs.Builder.px(ttl.toMillis()));
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache PUT failed for " + key, t);
        }
    }

    @Override
    public void delete(@NotNull String key) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return;
        try {
            cmd.del(key.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache DEL failed for " + key, t);
        }
    }

    @Override
    public boolean exists(@NotNull String key) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return false;
        try {
            return cmd.exists(key.getBytes(StandardCharsets.UTF_8)) > 0;
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache EXISTS failed for " + key, t);
            return false;
        }
    }

    @Override
    public @NotNull Map<String, String> getHash(@NotNull String key) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return Collections.emptyMap();
        try {
            Map<byte[], byte[]> raw = cmd.hgetall(key.getBytes(StandardCharsets.UTF_8));
            if (raw.isEmpty()) return Collections.emptyMap();
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<byte[], byte[]> e : raw.entrySet()) {
                out.put(new String(e.getKey(), StandardCharsets.UTF_8),
                        new String(e.getValue(), StandardCharsets.UTF_8));
            }
            return out;
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache HGETALL failed for " + key, t);
            return Collections.emptyMap();
        }
    }

    @Override
    public void putHash(@NotNull String key, @NotNull Map<String, String> value, @NotNull Duration ttl) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return;
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        try {
            Map<byte[], byte[]> encoded = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : value.entrySet()) {
                encoded.put(e.getKey().getBytes(StandardCharsets.UTF_8),
                        e.getValue().getBytes(StandardCharsets.UTF_8));
            }
            cmd.del(keyBytes);
            if (!encoded.isEmpty()) {
                cmd.hmset(keyBytes, encoded);
            }
            cmd.pexpire(keyBytes, ttl.toMillis());
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache HMSET failed for " + key, t);
        }
    }

    @Override
    public @NotNull List<String> scanKeys(@NotNull String prefix, int limit) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        try {
            ScanArgs args = ScanArgs.Builder.matches(prefix + "*").limit(Math.min(limit, 256));
            ScanCursor cursor = ScanCursor.INITIAL;
            while (result.size() < limit) {
                KeyScanCursor<byte[]> next = cmd.scan(cursor, args);
                for (byte[] key : next.getKeys()) {
                    if (result.size() >= limit) break;
                    result.add(new String(key, StandardCharsets.UTF_8));
                }
                if (next.isFinished()) break;
                cursor = next;
            }
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache SCAN failed for " + prefix, t);
        }
        return result;
    }

    @Override
    public @NotNull Optional<FleetLock> tryLock(@NotNull String key, @NotNull Duration ttl) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return Optional.empty();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] holderBytes = instanceId.toString().getBytes(StandardCharsets.UTF_8);
        try {
            String ok = cmd.set(keyBytes, holderBytes,
                    SetArgs.Builder.nx().px(ttl.toMillis()));
            if (!"OK".equals(ok)) return Optional.empty();
            return Optional.of(new RedisLock(key, ttl));
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache SET NX EX failed for " + key, t);
            return Optional.empty();
        }
    }

    @Override
    public void publish(@NotNull String topic, byte @NotNull [] payload) {
        RedisCommands<byte[], byte[]> cmd = commandsOrNull();
        if (cmd == null) return;
        try {
            cmd.publish(topic.getBytes(StandardCharsets.UTF_8), payload);
        } catch (Throwable t) {
            logger.log(Level.FINE, "FleetCache PUBLISH failed for " + topic, t);
        }
    }

    @Override
    public @NotNull AutoCloseable subscribe(@NotNull String topic, @NotNull Consumer<byte[]> handler) {
        SubscriberSet set = subscriptions.computeIfAbsent(topic, k -> new SubscriberSet());
        synchronized (set) {
            set.handlers.add(handler);
            ensurePubSubAttached();
            if (!set.subscribed) {
                StatefulRedisPubSubConnection<byte[], byte[]> pubSub = pubSubOrNull();
                if (pubSub != null) {
                    try {
                        pubSub.sync().subscribe(topic.getBytes(StandardCharsets.UTF_8));
                        set.subscribed = true;
                    } catch (Throwable t) {
                        logger.log(Level.FINE, "FleetCache SUBSCRIBE failed for " + topic, t);
                    }
                }
            }
        }
        return () -> cancelSubscription(topic, handler);
    }

    private void cancelSubscription(String topic, Consumer<byte[]> handler) {
        SubscriberSet set = subscriptions.get(topic);
        if (set == null) return;
        synchronized (set) {
            set.handlers.remove(handler);
            if (set.handlers.isEmpty() && set.subscribed) {
                StatefulRedisPubSubConnection<byte[], byte[]> pubSub = pubSubOrNull();
                if (pubSub != null) {
                    try {
                        pubSub.sync().unsubscribe(topic.getBytes(StandardCharsets.UTF_8));
                    } catch (Throwable t) {
                        logger.log(Level.FINE, "FleetCache UNSUBSCRIBE failed for " + topic, t);
                    }
                }
                set.subscribed = false;
                subscriptions.remove(topic, set);
            }
        }
    }

    private void ensurePubSubAttached() {
        if (pubSubAttached) return;
        synchronized (pubSubLock) {
            if (pubSubAttached) return;
            StatefulRedisPubSubConnection<byte[], byte[]> pubSub = pubSubOrNull();
            if (pubSub == null) return;
            pubSub.addListener(new RedisPubSubAdapter<>() {
                @Override
                public void message(byte[] channel, byte[] message) {
                    String topic = new String(channel, StandardCharsets.UTF_8);
                    SubscriberSet set = subscriptions.get(topic);
                    if (set == null) return;
                    for (Consumer<byte[]> handler : set.snapshot()) {
                        try {
                            handler.accept(message);
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "FleetCache subscriber for " + topic + " threw", t);
                        }
                    }
                }
            });
            pubSubAttached = true;
        }
    }

    private RedisCommands<byte[], byte[]> commandsOrNull() {
        RedisConnection conn = redis.connection();
        return conn == null ? null : conn.commands().sync();
    }

    private StatefulRedisPubSubConnection<byte[], byte[]> pubSubOrNull() {
        RedisConnection conn = redis.connection();
        return conn == null ? null : conn.pubSub();
    }

    /**
     * Loader-driven shutdown notification. Cancels every active subscription so reattach
     * after a reconnect doesn't accumulate ghost handlers.
     */
    public void detach() {
        for (Map.Entry<String, SubscriberSet> entry : subscriptions.entrySet()) {
            synchronized (entry.getValue()) {
                if (!entry.getValue().subscribed) continue;
                StatefulRedisPubSubConnection<byte[], byte[]> pubSub = pubSubOrNull();
                if (pubSub != null) {
                    try {
                        pubSub.sync().unsubscribe(entry.getKey().getBytes(StandardCharsets.UTF_8));
                    } catch (Throwable ignored) {
                    }
                }
                entry.getValue().subscribed = false;
                entry.getValue().handlers.clear();
            }
        }
        subscriptions.clear();
        pubSubAttached = false;
    }

    private static final class SubscriberSet {
        private final List<Consumer<byte[]>> handlers = new ArrayList<>();
        private boolean subscribed;

        synchronized List<Consumer<byte[]>> snapshot() {
            return new ArrayList<>(handlers);
        }
    }

    private final class RedisLock implements FleetLock {
        private final String key;
        private final Instant acquiredAt;
        private volatile Instant expiresAt;
        private volatile boolean closed;

        RedisLock(String key, Duration ttl) {
            this.key = key;
            this.acquiredAt = Instant.now();
            this.expiresAt = acquiredAt.plus(ttl);
        }

        @Override
        public @NotNull UUID holder() {
            return instanceId;
        }

        @Override
        public @NotNull Instant expiresAt() {
            return expiresAt;
        }

        @Override
        public boolean refresh(@NotNull Duration ttl) {
            if (closed) return false;
            RedisCommands<byte[], byte[]> cmd = commandsOrNull();
            if (cmd == null) return false;
            try {
                Boolean ok = cmd.pexpire(key.getBytes(StandardCharsets.UTF_8), ttl.toMillis());
                if (Boolean.TRUE.equals(ok)) {
                    expiresAt = Instant.now().plus(ttl);
                    return true;
                }
            } catch (Throwable ignored) {
            }
            return false;
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            RedisCommands<byte[], byte[]> cmd = commandsOrNull();
            if (cmd == null) return;
            try {
                // Best-effort release: delete the key only if we still hold it. Avoid using
                // raw eval scripts for simplicity; a stolen lock just expires on its own.
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                byte[] current = cmd.get(keyBytes);
                if (current != null && new String(current, StandardCharsets.UTF_8).equals(instanceId.toString())) {
                    cmd.del(keyBytes);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
