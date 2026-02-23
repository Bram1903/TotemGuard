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

import com.deathmotion.totemguard.api3.redis.RedisRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.event.Event;
import io.lettuce.core.event.connection.ConnectedEvent;
import io.lettuce.core.event.connection.DisconnectedEvent;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RedisRepositoryImpl implements RedisRepository {

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private @Nullable ClientResources resources;
    private @Nullable RedisClient client;

    private @Nullable StatefulRedisConnection<byte[], byte[]> connection;
    private @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection;

    public RedisRepositoryImpl() {
        start();
    }

    public synchronized void start() {
        final RedisOptions options = new RedisOptions();

        if (!options.isEnabled()) return;
        if (client != null) return;

        RedisURI uri = buildUri(options);

        resources = ClientResources.builder().build();
        client = RedisClient.create(resources, uri);

        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .build());

        resources.eventBus().get().subscribe(this::onEvent);

        try {
            connection = client.connect(new ByteArrayCodec());
            pubSubConnection = client.connectPubSub(new ByteArrayCodec());

            connected.set(connection.isOpen() && pubSubConnection.isOpen());
        } catch (Exception e) {
            TGPlatform.getInstance().getLogger().warning("Failed to connect to Redis: " + e.getMessage());
            connected.set(false);
            stop();
        }
    }

    @Blocking
    public synchronized void restart() {
        stop();
        start();
    }

    public synchronized void stop() {
        connected.set(false);

        if (pubSubConnection != null) {
            try { pubSubConnection.close(); } catch (Exception ignored) {}
            pubSubConnection = null;
        }

        if (connection != null) {
            try { connection.close(); } catch (Exception ignored) {}
            connection = null;
        }

        if (client != null) {
            try { client.shutdown(); } catch (Exception ignored) {}
            client = null;
        }

        if (resources != null) {
            try { resources.shutdown(); } catch (Exception ignored) {}
            resources = null;
        }
    }

    public boolean isConnected() {
        var conn = connection;
        var ps = pubSubConnection;
        return connected.get()
                && conn != null && conn.isOpen()
                && ps != null && ps.isOpen();
    }

    public @Nullable StatefulRedisConnection<byte[], byte[]> connection() {
        return connection;
    }

    public @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection() {
        return pubSubConnection;
    }

    private void onEvent(Event event) {
        if (event instanceof DisconnectedEvent) {
            connected.set(false);
        } else if (event instanceof ConnectedEvent) {
            var conn = connection;
            var ps = pubSubConnection;
            connected.set(conn != null && conn.isOpen() && ps != null && ps.isOpen());
        }
    }

    private static RedisURI buildUri(RedisOptions options) {
        RedisURI.Builder b = RedisURI.builder()
                .withHost(options.getHost())
                .withPort(options.getPort())
                .withTimeout(Duration.ofSeconds(5))
                .withAuthentication(options.getUsername(), options.getPassword());

        return b.build();
    }
}