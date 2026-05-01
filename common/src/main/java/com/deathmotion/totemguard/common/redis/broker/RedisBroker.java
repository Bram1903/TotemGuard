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

package com.deathmotion.totemguard.common.redis.broker;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.RedisOptions;
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketCodec;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketRegistry;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RedisBroker extends RedisPubSubAdapter<byte[], byte[]> implements ConnectionStateListener {

    private final Logger logger = TGPlatform.getInstance().getLogger();
    private final PacketRegistry registry;
    private final String identifier;
    private final EnumMap<MessagingTopic, byte[]> channelBytesByTopic;
    private final byte[][] subscribedChannels;

    private final Object lock = new Object();
    private volatile @Nullable RedisConnection connection;

    public RedisBroker(PacketRegistry registry, String identifier, RedisOptions.MessagingOptions options) {
        this.registry = registry;
        this.identifier = identifier;
        this.channelBytesByTopic = new EnumMap<>(MessagingTopic.class);

        // De-dupe so colliding topic channels (e.g. alerts.channel: "updates")
        // don't get subscribed twice. Packet ID disambiguates on receive.
        Set<String> uniqueChannels = new LinkedHashSet<>();
        for (MessagingTopic topic : MessagingTopic.values()) {
            String channelName = options.channelFor(topic);
            channelBytesByTopic.put(topic, channelName.getBytes(StandardCharsets.UTF_8));
            uniqueChannels.add(channelName);
        }
        this.subscribedChannels = uniqueChannels.stream()
                .map(name -> name.getBytes(StandardCharsets.UTF_8))
                .toArray(byte[][]::new);
    }

    @Override
    public void onConnected(RedisConnection conn) {
        StatefulRedisPubSubConnection<byte[], byte[]> pubSub = conn.pubSub();
        synchronized (lock) {
            try {
                pubSub.removeListener(this);
            } catch (Exception ignored) {
            }
            pubSub.addListener(this);
            this.connection = conn;
        }

        pubSub.async().subscribe(subscribedChannels).whenComplete((ignored, error) -> {
            if (error == null) return;
            logger.log(Level.WARNING,
                    "Failed to subscribe to TotemGuard Redis channels: " + error.getMessage());
        });
    }

    @Override
    public void onDisconnected() {
        synchronized (lock) {
            this.connection = null;
        }
    }

    public <T> CompletionStage<Boolean> publish(Packet<T> packet, T payload) {
        RedisConnection current = this.connection;
        if (current == null || !current.isOpen()) {
            return CompletableFuture.completedFuture(false);
        }

        byte[] channel = channelBytesByTopic.get(packet.topic());
        if (channel == null) {
            logger.warning("No Redis channel resolved for topic " + packet.topic()
                    + "; dropping packet " + packet.getClass().getSimpleName());
            return CompletableFuture.completedFuture(false);
        }

        byte[] frame;
        try {
            frame = PacketCodec.encode(identifier, packet, payload);
        } catch (Exception ex) {
            logger.log(Level.WARNING,
                    "Failed to encode Redis packet " + packet.getClass().getSimpleName(), ex);
            return CompletableFuture.completedFuture(false);
        }

        return current.commands().async().publish(channel, frame)
                .toCompletableFuture()
                .handle((received, error) -> {
                    if (error != null) {
                        logger.log(Level.WARNING,
                                "Failed to publish Redis packet " + packet.getClass().getSimpleName()
                                        + ": " + error.getMessage());
                        return false;
                    }
                    return true;
                });
    }

    @Override
    public void message(byte[] channel, byte[] message) {
        if (channel == null || message == null) return;
        if (channel.length == 0 || message.length == 0) return;

        try {
            PacketCodec.Frame frame = PacketCodec.decode(message);
            if (frame.protocolVersion() != PacketCodec.PROTOCOL_VERSION) {
                logger.warning("Ignoring Redis packet with unsupported protocol version "
                        + frame.protocolVersion() + ".");
                return;
            }
            if (identifier.equals(frame.senderId())) {
                return;
            }
            registry.dispatch(frame.packetId(), frame.payload());
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to read Redis message", ex);
        }
    }
}
