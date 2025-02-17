/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.messaging.impl;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.messaging.ProxyAlertMessenger;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RedisProxyMessenger extends RedisPubSubAdapter<byte[], byte[]> implements ProxyAlertMessenger {
    private final @NotNull TotemGuard plugin;
    private final @NotNull String identifier;

    private RedisClient redisClient = null;
    private StatefulRedisPubSubConnection<byte[], byte[]> pubsub = null;
    private StatefulRedisConnection<byte[], byte[]> publishConnection = null;
    private byte[] channel;

    public RedisProxyMessenger(@NotNull TotemGuard plugin) {
        this.plugin = plugin;
        this.identifier = UUID.randomUUID().toString();
    }

    @Override
    public void start() {
        Settings.ProxyAlerts proxySettings = plugin.getConfigManager().getSettings().getProxy();
        channel = proxySettings.getChannel().getBytes(StandardCharsets.UTF_8);
        final Settings.ProxyAlerts.RedisConfiguration settings = proxySettings.getRedis();

        try {
            redisClient = RedisClient.create(
                    RedisURI.builder()
                            .withHost(settings.getHost())
                            .withPort(settings.getPort())
                            .withAuthentication(settings.getUsername(), settings.getPassword())
                            .build()
            );

            // Create pubsub connection for subscribing
            this.pubsub = redisClient.connectPubSub(new ByteArrayCodec());
            this.pubsub.async().subscribe(channel);
            this.pubsub.addListener(this);

            // Create a separate connection for publishing
            this.publishConnection = redisClient.connect(new ByteArrayCodec());
            plugin.getLogger().info("Successfully connected to Redis.");
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to connect to Redis.");
            exception.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (redisClient != null) {
            redisClient.shutdown();
            plugin.debug("RedisClient and all connections shut down successfully!");
        } else {
            plugin.debug("RedisClient was already null or not initialized.");
        }
    }

    @Override
    public void message(byte[] channel, byte[] bytes) {
        plugin.debug("Received message from Redis.");
        if (!java.util.Arrays.equals(channel, this.channel)) return;

        ByteArrayDataInput out = ByteStreams.newDataInput(bytes);
        final String from = out.readUTF();

        // Shouldn't repeat if we receive from ourselves
        if (from.equals(this.identifier)) {
            return;
        }

        final String json = out.readUTF();
        final Component alert = GsonComponentSerializer.gson().deserialize(json);

        this.plugin.getAlertManager().sendAlert(alert);
    }

    @Override
    public void sendAlert(@NotNull Component alert) {
        if (this.publishConnection == null) {
            plugin.debug("Redis is not connected, cannot send cross-server alerts.");
            return;
        }

        final String json = GsonComponentSerializer.gson().serialize(alert);
        ByteArrayDataOutput in = ByteStreams.newDataOutput();
        in.writeUTF(this.identifier);
        in.writeUTF(json);

        byte[] bytes = in.toByteArray();
        this.publishConnection.async().publish(channel, bytes)
                .exceptionally(ex -> {
                    plugin.debug("Failed to publish message.");
                    ex.printStackTrace();
                    return null;
                });
    }
}