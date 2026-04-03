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
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketRegistry;
import com.deathmotion.totemguard.common.redis.options.RedisOptions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RedisBroker extends RedisPubSubAdapter<byte[], byte[]> {

    private static final int PROTOCOL_VERSION = 1;

    private final Logger logger = TGPlatform.getInstance().getLogger();
    private final PacketRegistry registry;
    private final String identifier;
    private final String channelName;
    private final byte[] channel;

    private volatile @Nullable StatefulRedisConnection<byte[], byte[]> connection;
    private volatile @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection;

    public RedisBroker(PacketRegistry registry, String identifier, RedisOptions.MessagingOptions options) {
        this.registry = registry;
        this.identifier = identifier;
        this.channelName = options.getChannel() == null ? "" : options.getChannel().trim();
        this.channel = channelName.getBytes(StandardCharsets.UTF_8);
    }

    public void start(
            @Nullable StatefulRedisConnection<byte[], byte[]> connection,
            @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection
    ) {
        stop();

        this.connection = connection;
        this.pubSubConnection = pubSubConnection;

        if (!isConfigured() || pubSubConnection == null) {
            return;
        }

        pubSubConnection.addListener(this);

        try {
            pubSubConnection.async().subscribe(channel);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to subscribe to Redis channel " + channelName, exception);
        }
    }

    public void stop() {
        StatefulRedisPubSubConnection<byte[], byte[]> currentPubSubConnection = this.pubSubConnection;

        this.connection = null;
        this.pubSubConnection = null;

        if (currentPubSubConnection == null) {
            return;
        }

        try {
            currentPubSubConnection.removeListener(this);
        } catch (Exception ignored) {
        }

        if (!isConfigured() || !currentPubSubConnection.isOpen()) {
            return;
        }

        try {
            currentPubSubConnection.async().unsubscribe(channel);
        } catch (Exception ignored) {
        }
    }

    public <T> boolean publish(Packet<T> packet, T payload) {
        if (!isConfigured()) {
            return false;
        }

        StatefulRedisConnection<byte[], byte[]> currentConnection = this.connection;
        if (currentConnection == null || !currentConnection.isOpen()) {
            return false;
        }

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeByte(PROTOCOL_VERSION);
        output.writeUTF(identifier);
        output.writeInt(packet.getId());
        packet.writeData(output, payload);

        try {
            currentConnection.async().publish(channel, output.toByteArray());
            return true;
        } catch (Exception exception) {
            logger.log(
                    Level.WARNING,
                    "Failed to publish Redis packet " + packet.getClass().getSimpleName() + " to channel " + channelName,
                    exception
            );
            return false;
        }
    }

    @Override
    public void message(byte[] channelBytes, byte[] messageBytes) {
        if (!isExpectedMessage(channelBytes, messageBytes)) {
            return;
        }

        try {
            ByteArrayDataInput input = ByteStreams.newDataInput(messageBytes);

            int protocolVersion = input.readUnsignedByte();
            if (protocolVersion != PROTOCOL_VERSION) {
                logger.warning(
                        "Ignoring Redis packet with unsupported protocol version " + protocolVersion +
                                " on channel " + channelName + "."
                );
                return;
            }

            String senderIdentifier = input.readUTF();
            if (identifier.equals(senderIdentifier)) {
                return;
            }

            int packetId = input.readInt();
            registry.handlePacket(packetId, input);
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Failed to read Redis message from channel " + channelName, exception);
        }
    }

    private boolean isConfigured() {
        return channel.length > 0;
    }

    private boolean isExpectedMessage(byte[] channelBytes, byte[] messageBytes) {
        return channelBytes != null
                && messageBytes != null
                && channelBytes.length > 0
                && messageBytes.length > 0
                && Arrays.equals(channelBytes, channel);
    }
}
