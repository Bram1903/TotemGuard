/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class RedisProxyMessenger implements ProxyAlertMessenger {
    private final @NotNull TotemGuard plugin;
    private final @NotNull String identifier;

    private JedisPool jedisPool = null;
    private Jedis publishJedis = null;
    private byte[] channel;
    private Thread subscribeThread;
    private BinaryJedisPubSub jedisPubSub;

    public RedisProxyMessenger(@NotNull TotemGuard plugin) {
        this.plugin = plugin;
        this.identifier = UUID.randomUUID().toString();
    }

    @Override
    public void start() {
        Settings.ProxyAlerts proxySettings = plugin.getConfigManager().getSettings().getProxyAlerts();
        channel = proxySettings.getChannel().getBytes(StandardCharsets.UTF_8);
        final Settings.ProxyAlerts.RedisConfiguration settings = proxySettings.getRedis();

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            HostAndPort hostAndPort = new HostAndPort(settings.getHost(), settings.getPort());
            DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder().timeoutMillis(2000);
            clientConfigBuilder.user(settings.getUsername());
            clientConfigBuilder.password(settings.getPassword());
            JedisClientConfig clientConfig = clientConfigBuilder.build();

            jedisPool = new JedisPool(poolConfig, hostAndPort, clientConfig);
            publishJedis = jedisPool.getResource();
            plugin.getLogger().info("Successfully connected to Redis.");

            // Create JedisPubSub instance
            jedisPubSub = new BinaryJedisPubSub() {
                @Override
                public void onMessage(byte[] channel, byte[] message) {
                    handleMessage(channel, message);
                }
            };

            // Start subscription in a new thread
            subscribeThread = new Thread(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(jedisPubSub, channel);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error while subscribing to Redis channel.");
                    e.printStackTrace();
                }
            });
            subscribeThread.start();

        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to connect to Redis.");
            exception.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (jedisPubSub != null) {
            try {
                jedisPubSub.unsubscribe();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (subscribeThread != null) {
            try {
                subscribeThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (publishJedis != null) {
            publishJedis.close();
        }

        if (jedisPool != null) {
            jedisPool.close();
            plugin.debug("JedisPool and all connections shut down successfully!");
        } else {
            plugin.debug("JedisPool was already null or not initialized.");
        }
    }

    private void handleMessage(byte[] channel, byte[] message) {
        if (!Arrays.equals(channel, this.channel)) return;

        ByteArrayDataInput out = ByteStreams.newDataInput(message);
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
        if (this.publishJedis == null) {
            plugin.debug("Redis is not connected, cannot send cross-server alerts.");
            return;
        }

        final String json = GsonComponentSerializer.gson().serialize(alert);
        ByteArrayDataOutput in = ByteStreams.newDataOutput();
        in.writeUTF(this.identifier);
        in.writeUTF(json);

        byte[] bytes = in.toByteArray();
        try {
            publishJedis.publish(channel, bytes);
        } catch (Exception ex) {
            plugin.debug("Failed to publish message.");
            ex.printStackTrace();
        }
    }
}


