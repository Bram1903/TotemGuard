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

package com.deathmotion.totemguard.common.redis;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.redis.options.RedisOptions;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

final class RedisConnectionManager {

    private static final ByteArrayCodec CODEC = new ByteArrayCodec();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    private final Object lock = new Object();

    private volatile @Nullable ConnectionHandle handle;

    @Blocking
    void start(RedisOptions options) {
        synchronized (lock) {
            if (handle != null) {
                return;
            }

            handle = open(options);
        }
    }

    @Blocking
    void stop() {
        close(swap(null));
    }

    @Blocking
    void restart(RedisOptions options) {
        close(swap(null));
        start(options);
    }

    boolean isConnected() {
        ConnectionHandle currentHandle = handle;
        return currentHandle != null && currentHandle.isConnected();
    }

    @Nullable StatefulRedisConnection<byte[], byte[]> connection() {
        ConnectionHandle currentHandle = handle;
        return currentHandle == null ? null : currentHandle.connection();
    }

    @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection() {
        ConnectionHandle currentHandle = handle;
        return currentHandle == null ? null : currentHandle.pubSubConnection();
    }

    private @Nullable ConnectionHandle swap(@Nullable ConnectionHandle newHandle) {
        synchronized (lock) {
            ConnectionHandle previous = this.handle;
            this.handle = newHandle;
            return previous;
        }
    }

    private @Nullable ConnectionHandle open(RedisOptions options) {
        ClientResources resources = ClientResources.create();
        RedisClient client = RedisClient.create(resources, buildUri(options));
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .build());

        RedisConnectionEventLogger eventLogger = new RedisConnectionEventLogger();
        eventLogger.start(resources);

        try {
            StatefulRedisConnection<byte[], byte[]> connection = client.connect(CODEC);
            StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection = client.connectPubSub(CODEC);
            eventLogger.markUp();

            return new ConnectionHandle(resources, client, connection, pubSubConnection, eventLogger);
        } catch (Exception exception) {
            TGPlatform.getInstance().getLogger().warning("Failed to connect to Redis: " + exception.getMessage());
            close(new ConnectionHandle(resources, client, null, null, eventLogger));
            return null;
        }
    }

    private RedisURI buildUri(RedisOptions options) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(options.getHost())
                .withPort(options.getPort())
                .withTimeout(CONNECT_TIMEOUT);

        String username = options.getUsername();
        String password = options.getPassword();

        if (password != null && !password.isBlank()) {
            if (username != null && !username.isBlank()) {
                builder.withAuthentication(username, password);
            } else {
                builder.withPassword(password.toCharArray());
            }
        }

        return builder.build();
    }

    private void close(@Nullable ConnectionHandle handle) {
        if (handle == null) {
            return;
        }

        handle.close();
    }

    private record ConnectionHandle(
            ClientResources resources,
            RedisClient client,
            @Nullable StatefulRedisConnection<byte[], byte[]> connection,
            @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection,
            RedisConnectionEventLogger eventLogger
    ) {

        boolean isConnected() {
            return connection != null && connection.isOpen() && eventLogger.isUp();
        }

        void close() {
            eventLogger.markDown();
            closeQuietly(eventLogger);
            closeQuietly(pubSubConnection);
            closeQuietly(connection);
            shutdownQuietly(client);
            shutdownQuietly(resources);
        }

        private static void closeQuietly(@Nullable AutoCloseable closeable) {
            if (closeable == null) {
                return;
            }

            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }

        private static void shutdownQuietly(RedisClient client) {
            try {
                client.shutdown();
            } catch (Exception ignored) {
            }
        }

        private static void shutdownQuietly(ClientResources resources) {
            try {
                resources.shutdown();
            } catch (Exception ignored) {
            }
        }
    }
}
