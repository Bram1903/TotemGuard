package com.deathmotion.totemguard.messaging;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.config.Settings;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
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

    private StatefulRedisPubSubConnection<byte[], byte[]> pubsub = null;
    private byte[] channel = "totemguard".getBytes(StandardCharsets.UTF_8);

    public RedisProxyMessenger(@NotNull TotemGuard plugin) {
        this.plugin = plugin;
        this.identifier = UUID.randomUUID().toString();
    }

    @Override
    public void start() {
        final Settings.ProxyAlerts.RedisConfiguration settings = plugin.getConfigManager()
            .getSettings()
            .getProxyAlerts()
            .getRedis();

        try {
            this.pubsub = RedisClient.create(
                RedisURI.builder()
                    .withHost(settings.getHost())
                    .withPort(settings.getPort())
                    .withAuthentication(settings.getUsername(), settings.getPassword())
                    .build()
            ).connectPubSub(new ByteArrayCodec());

            this.pubsub.async().subscribe(channel);
            this.pubsub.addListener(this);
        } catch (Exception exception) {
            plugin.debug("Failed to connect to Redis.");
            exception.printStackTrace();
        }
        this.channel = settings.getChannel().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void stop() {
        if (this.pubsub != null) {
            this.pubsub.async().unsubscribe(channel)
                .thenAccept((e) -> this.pubsub.closeAsync())
                .thenAccept((e) -> plugin.debug("Disconnected from redis pubsub successfully!"))
                .exceptionally((ex) -> {
                    plugin.debug("Failed to disconnect gracefully from redis pubsub.");
                    ex.printStackTrace();
                    return null;
                });
        }
    }

    @Override
    public void message(byte[] channel, byte[] bytes) {
        if (channel != this.channel) return;

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
        final String json = GsonComponentSerializer.gson().serialize(alert);
        ByteArrayDataOutput in = ByteStreams.newDataOutput();
        in.writeUTF(this.identifier);
        in.writeUTF(json);

        byte[] bytes = in.toByteArray();
        this.pubsub.async().publish(channel, bytes);
    }
}
