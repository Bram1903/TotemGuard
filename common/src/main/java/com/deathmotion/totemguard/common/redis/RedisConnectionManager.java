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
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

final class RedisConnectionManager {

    private final Object lock = new Object();

    private volatile @Nullable ClientResources resources;
    private volatile @Nullable RedisClient client;
    private volatile @Nullable StatefulRedisConnection<byte[], byte[]> connection;
    private volatile @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection;

    @Blocking
    void start() {
        synchronized (lock) {
            if (client != null) return;

            var uri = RedisUriFactory.build(new RedisOptions());

            ClientResources res = RedisClientFactory.createResources();
            RedisClient cli = RedisClientFactory.createClient(res, uri);

            try {
                StatefulRedisConnection<byte[], byte[]> conn = cli.connect(new ByteArrayCodec());
                StatefulRedisPubSubConnection<byte[], byte[]> ps = cli.connectPubSub(new ByteArrayCodec());

                resources = res;
                client = cli;
                connection = conn;
                pubSubConnection = ps;
            } catch (Exception e) {
                try {
                    cli.shutdown();
                } catch (Exception ignored) {
                }
                try {
                    res.shutdown();
                } catch (Exception ignored) {
                }

                TGPlatform.getInstance().getLogger().warning("Failed to connect to Redis: " + e.getMessage());
            }
        }
    }

    @Blocking
    void stop() {
        synchronized (lock) {
            var ps = pubSubConnection;
            pubSubConnection = null;
            var conn = connection;
            connection = null;
            var cli = client;
            client = null;
            var res = resources;
            resources = null;

            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception ignored) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {
                }
            }
            if (cli != null) {
                try {
                    cli.shutdown();
                } catch (Exception ignored) {
                }
            }
            if (res != null) {
                try {
                    res.shutdown();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Blocking
    void restart() {
        synchronized (lock) {
            stop();
            start();
        }
    }

    boolean isConnected() {
        var conn = connection;
        var ps = pubSubConnection;
        return conn != null && conn.isOpen()
                && ps != null && ps.isOpen();
    }

    @Nullable StatefulRedisConnection<byte[], byte[]> connection() {
        return connection;
    }

    @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection() {
        return pubSubConnection;
    }
}
