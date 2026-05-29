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

package com.deathmotion.totemguard.common.cluster;

import com.deathmotion.totemguard.api.cluster.ClusterLease;
import com.deathmotion.totemguard.api.cluster.ClusterService;
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Redis-backed implementation of the public {@link ClusterService}. Leases use SET-NX-PX with
 * an ownership-checked renew, and pub/sub mirrors the fleet cache's pattern and re-subscribes
 * across reconnects. When Redis is offline, leases are granted locally and pub/sub is inert.
 */
public final class ClusterServiceImpl implements ClusterService, ConnectionStateListener {

    private static final String LEASE_PREFIX = "totemguard:lease:";

    private final RedisRepositoryImpl redis;
    private final String holderId;
    private final Logger logger;

    private final Map<String, SubscriberSet> subscriptions = new ConcurrentHashMap<>();
    private final Object pubSubLock = new Object();
    private volatile boolean pubSubAttached;

    public ClusterServiceImpl(@NotNull RedisRepositoryImpl redis, @NotNull UUID holderId, @NotNull Logger logger) {
        this.redis = redis;
        this.holderId = holderId.toString();
        this.logger = logger;
        redis.addStateListener(this);
    }

    @Override
    public boolean isConnected() {
        return redis.isConnected();
    }

    @Override
    public @NotNull Optional<ClusterLease> acquireLease(@NotNull String name, @NotNull Duration ttl) {
        RedisCommands<byte[], byte[]> commands = commandsOrNull();
        if (commands == null) {
            return Optional.of(new LocalLease(name));
        }
        try {
            String result = commands.set(leaseKey(name), holderBytes(), SetArgs.Builder.nx().px(ttl.toMillis()));
            return "OK".equals(result) ? Optional.of(new RedisLease(name)) : Optional.empty();
        } catch (Throwable t) {
            logger.log(Level.FINE, "Cluster lease acquire failed for " + name, t);
            return Optional.empty();
        }
    }

    @Override
    public void publish(@NotNull String channel, byte @NotNull [] message) {
        RedisCommands<byte[], byte[]> commands = commandsOrNull();
        if (commands == null) return;
        try {
            commands.publish(channel.getBytes(StandardCharsets.UTF_8), message);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Cluster publish failed for " + channel, t);
        }
    }

    @Override
    public @NotNull AutoCloseable subscribe(@NotNull String channel, @NotNull Consumer<byte @NotNull []> handler) {
        SubscriberSet set = subscriptions.computeIfAbsent(channel, k -> new SubscriberSet());
        synchronized (set) {
            set.handlers.add(handler);
            ensurePubSubAttached();
            subscribeChannel(channel, set);
        }
        return () -> cancel(channel, handler);
    }

    @Override
    public void onConnected(RedisConnection connection) {
        synchronized (pubSubLock) {
            pubSubAttached = false;
        }
        ensurePubSubAttached();
        for (Map.Entry<String, SubscriberSet> entry : subscriptions.entrySet()) {
            synchronized (entry.getValue()) {
                entry.getValue().subscribed = false;
                subscribeChannel(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void onDisconnected() {
        synchronized (pubSubLock) {
            pubSubAttached = false;
        }
        for (SubscriberSet set : subscriptions.values()) {
            synchronized (set) {
                set.subscribed = false;
            }
        }
    }

    private void subscribeChannel(String channel, SubscriberSet set) {
        if (set.subscribed || set.handlers.isEmpty()) return;
        StatefulRedisPubSubConnection<byte[], byte[]> pubSub = pubSubOrNull();
        if (pubSub == null) return;
        try {
            pubSub.sync().subscribe(channel.getBytes(StandardCharsets.UTF_8));
            set.subscribed = true;
        } catch (Throwable t) {
            logger.log(Level.FINE, "Cluster subscribe failed for " + channel, t);
        }
    }

    private void cancel(String channel, Consumer<byte[]> handler) {
        SubscriberSet set = subscriptions.get(channel);
        if (set == null) return;
        synchronized (set) {
            set.handlers.remove(handler);
            if (set.handlers.isEmpty()) {
                StatefulRedisPubSubConnection<byte[], byte[]> pubSub = pubSubOrNull();
                if (pubSub != null && set.subscribed) {
                    try {
                        pubSub.sync().unsubscribe(channel.getBytes(StandardCharsets.UTF_8));
                    } catch (Throwable ignored) {
                    }
                }
                subscriptions.remove(channel, set);
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
                    SubscriberSet set = subscriptions.get(new String(channel, StandardCharsets.UTF_8));
                    if (set == null) return;
                    for (Consumer<byte[]> handler : set.snapshot()) {
                        try {
                            handler.accept(message);
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "Cluster subscriber threw", t);
                        }
                    }
                }
            });
            pubSubAttached = true;
        }
    }

    private RedisCommands<byte[], byte[]> commandsOrNull() {
        if (!redis.isConnected()) return null;
        RedisConnection connection = redis.connection();
        return connection == null ? null : connection.commands().sync();
    }

    private StatefulRedisPubSubConnection<byte[], byte[]> pubSubOrNull() {
        RedisConnection connection = redis.connection();
        return connection == null ? null : connection.pubSub();
    }

    private byte[] leaseKey(String name) {
        return (LEASE_PREFIX + name).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] holderBytes() {
        return holderId.getBytes(StandardCharsets.UTF_8);
    }

    private static final class SubscriberSet {
        private final List<Consumer<byte[]>> handlers = new ArrayList<>();
        private boolean subscribed;

        synchronized List<Consumer<byte[]>> snapshot() {
            return new ArrayList<>(handlers);
        }
    }

    private record LocalLease(String name) implements ClusterLease {
        @Override
        public boolean renew(@NotNull Duration ttl) {
            return true;
        }

        @Override
        public void release() {
        }
    }

    private final class RedisLease implements ClusterLease {
        private final String name;
        private volatile boolean released;

        private RedisLease(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public boolean renew(@NotNull Duration ttl) {
            if (released) return false;
            RedisCommands<byte[], byte[]> commands = commandsOrNull();
            if (commands == null) return false;
            try {
                byte[] key = leaseKey(name);
                byte[] current = commands.get(key);
                if (current == null) {
                    return "OK".equals(commands.set(key, holderBytes(), SetArgs.Builder.nx().px(ttl.toMillis())));
                }
                if (!holderId.equals(new String(current, StandardCharsets.UTF_8))) {
                    return false;
                }
                commands.pexpire(key, ttl.toMillis());
                return true;
            } catch (Throwable t) {
                logger.log(Level.FINE, "Cluster lease renew failed for " + name, t);
                return false;
            }
        }

        @Override
        public void release() {
            if (released) return;
            released = true;
            RedisCommands<byte[], byte[]> commands = commandsOrNull();
            if (commands == null) return;
            try {
                byte[] key = leaseKey(name);
                byte[] current = commands.get(key);
                if (current != null && holderId.equals(new String(current, StandardCharsets.UTF_8))) {
                    commands.del(key);
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "Cluster lease release failed for " + name, t);
            }
        }
    }
}
