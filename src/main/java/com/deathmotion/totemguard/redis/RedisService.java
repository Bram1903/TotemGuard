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

package com.deathmotion.totemguard.redis;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import com.deathmotion.totemguard.config.Settings;
import com.deathmotion.totemguard.interfaces.Reloadable;
import com.deathmotion.totemguard.redis.handlers.SyncAlertMessageHandler;
import com.deathmotion.totemguard.redis.packet.Packet;
import com.deathmotion.totemguard.redis.packet.PacketRegistry;
import com.deathmotion.totemguard.util.TGVersions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.deathmotion.totemguard.redis.packet.Packet.readVersion;
import static com.deathmotion.totemguard.redis.packet.Packet.writeVersion;

@Getter
public class RedisService extends RedisPubSubAdapter<byte[], byte[]> implements Reloadable {
    private final TotemGuard plugin;
    private final PacketRegistry registry;
    private final List<Reloadable> handlers = new ArrayList<>();
    private final String identifier;

    private @Nullable RedisClient client = null;
    private @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubsub = null;
    private @Nullable StatefulRedisConnection<byte[], byte[]> publishConnection = null;
    private byte @Nullable [] channel;

    public RedisService(TotemGuard plugin) {
        this.plugin = plugin;
        this.registry = new PacketRegistry();
        this.identifier = UUID.randomUUID().toString();

        start();
        handlers.add(new SyncAlertMessageHandler(plugin, registry));
    }

    private void start() {
        Settings.Redis redisSettings = plugin.getConfigManager().getSettings().getRedis();
        if (!redisSettings.isEnabled()) return;

        client = createRedisClient(redisSettings);
        channel = redisSettings.getChannel().getBytes(StandardCharsets.UTF_8);

        pubsub = client.connectPubSub(new ByteArrayCodec());
        this.pubsub.async().subscribe(channel);
        this.pubsub.addListener(this);

        // Create a separate connection for publishing
        this.publishConnection = client.connect(new ByteArrayCodec());
    }

    public void stop() {
        if (pubsub != null) pubsub.close();
        if (publishConnection != null) publishConnection.close();
        if (client != null) client.shutdown();
    }

    @Override
    public void reload() {
        stop();
        start();
        handlers.forEach(Reloadable::reload);
    }

    @Override
    public void message(byte[] channelBytes, byte[] messageBytes) {
        if (channelBytes == null || messageBytes == null || channelBytes.length == 0 || messageBytes.length == 0)
            return;
        if (!Arrays.equals(channelBytes, this.channel)) return;

        ByteArrayDataInput out = ByteStreams.newDataInput(messageBytes);
        final String from = out.readUTF();

        // Shouldn't repeat if we receive from ourselves
        if (from.equals(this.identifier)) {
            return;
        }

        TGVersion version = readVersion(out);
        if (!TGVersions.CURRENT.equalsWithoutCommit(version)) {
            plugin.getLogger().warning("Received packet from Redis with incompatible version. Make sure all instances are running the same version.");
            return;
        }

        registry.handlePacket(out);
    }

    /**
     * Publishes a packet with the given payload. The identifier is prepended so that recipients can
     * ignore packets sent by this instance.
     *
     * @param packet the packet type
     * @param obj    the payload
     * @param <T>    the type of the payload
     */
    public <T> void publish(Packet<T> packet, T obj) {
        // Write the packet payload.
        ByteArrayDataOutput dataOutput = packet.write(obj);
        byte[] payload = dataOutput.toByteArray();

        // Prepend our identifier.
        ByteArrayDataOutput finalOutput = ByteStreams.newDataOutput();
        finalOutput.writeUTF(this.identifier);
        writeVersion(finalOutput);
        finalOutput.write(payload);

        publish(finalOutput.toByteArray());
    }

    /**
     * Publishes raw data to the Redis channel.
     *
     * @param data the raw data bytes
     */
    public void publish(byte[] data) {
        if (publishConnection == null) {
            plugin.debug("Attempted to publish data to Redis, but the connection is not established.");
            return;
        }
        publishConnection.async().publish(channel, data);
    }

    private RedisClient createRedisClient(Settings.Redis redisSettings) {
        RedisURI.Builder uriBuilder = RedisURI.builder()
                .withHost(redisSettings.getHost())
                .withPort(redisSettings.getPort());

        // Optionally set authentication if provided
        if (redisSettings.getUsername() != null && !redisSettings.getUsername().isEmpty() &&
                redisSettings.getPassword() != null && !redisSettings.getPassword().isEmpty()) {
            uriBuilder.withAuthentication(redisSettings.getUsername(), redisSettings.getPassword());
        }

        return RedisClient.create(uriBuilder.build());
    }
}
