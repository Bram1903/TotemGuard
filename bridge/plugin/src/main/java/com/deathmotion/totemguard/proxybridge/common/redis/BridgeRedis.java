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

package com.deathmotion.totemguard.proxybridge.common.redis;

import com.deathmotion.totemguard.proxybridge.common.ProxyConfig;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class BridgeRedis {

    private final Logger logger;
    private final AtomicBoolean disconnected = new AtomicBoolean(true);

    private volatile ProxyConfig.Redis options;
    private volatile boolean shuttingDown;
    private @Nullable RedisClient client;
    private @Nullable StatefulRedisConnection<String, String> connection;
    private @Nullable StatefulRedisPubSubConnection<String, String> pubsub;

    public BridgeRedis(@NotNull ProxyConfig.Redis options, @NotNull Logger logger) {
        this.options = options;
        this.logger = logger;
    }

    private static void close(@Nullable AutoCloseable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    public synchronized void restart(@NotNull ProxyConfig.Redis newOptions) {
        stop();
        this.options = newOptions;
        start();
    }

    public synchronized void start() {
        if (client != null) return;
        this.shuttingDown = false;
        this.disconnected.set(true);
        this.client = RedisClient.create(buildUri());
        this.client.setOptions(buildClientOptions());
        observeConnectionLifecycle();
        this.connection = client.connect();
        this.pubsub = client.connectPubSub();
    }

    public synchronized void stop() {
        this.shuttingDown = true;
        close(pubsub);
        pubsub = null;
        close(connection);
        connection = null;
        if (client != null) {
            try {
                client.shutdown(Duration.ofMillis(100), Duration.ofSeconds(2));
            } catch (Exception ignored) {
            }
            client = null;
        }
    }

    public @Nullable StatefulRedisConnection<String, String> connection() {
        return connection;
    }

    public @Nullable StatefulRedisPubSubConnection<String, String> pubsub() {
        return pubsub;
    }

    private RedisURI buildUri() {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(options.host())
                .withPort(options.port())
                .withDatabase(options.database())
                .withSsl(options.tls());
        if (!options.password().isEmpty()) {
            if (!options.username().isEmpty()) {
                builder.withAuthentication(options.username(), options.password().toCharArray());
            } else {
                builder.withPassword(options.password().toCharArray());
            }
        }
        return builder.build();
    }

    private ClientOptions buildClientOptions() {
        return ClientOptions.builder()
                .autoReconnect(true)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .keepAlive(true)
                        .build())
                .build();
    }

    private void observeConnectionLifecycle() {
        if (client == null) return;
        client.getResources().eventBus().get().subscribe(event -> {
            if (shuttingDown) return;
            if (event instanceof ConnectedEvent) {
                if (disconnected.compareAndSet(true, false)) {
                    logger.info("Redis connection established.");
                }
            } else if (event instanceof DisconnectedEvent) {
                if (disconnected.compareAndSet(false, true)) {
                    logger.warning("Redis connection lost; auto-reconnect in progress, state will republish on the next tick.");
                }
            }
        });
    }
}
