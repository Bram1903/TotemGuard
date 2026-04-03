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
import com.deathmotion.totemguard.api3.reload.Reloadable;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.redis.broker.RedisBroker;
import com.deathmotion.totemguard.common.redis.broker.handlers.SyncAlertMessageHandler;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketRegistry;
import com.deathmotion.totemguard.common.redis.options.RedisOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class RedisRepositoryImpl implements RedisRepository {

    private final RedisConnectionManager manager;
    private final PacketRegistry registry;
    private final String identifier;
    private final List<Reloadable> handlers;

    private volatile @Nullable RedisOptions options;
    private volatile @Nullable RedisBroker broker;
    private volatile boolean enabled;

    public RedisRepositoryImpl() {
        this.manager = new RedisConnectionManager();
        this.registry = new PacketRegistry();
        this.identifier = UUID.randomUUID().toString();
        this.handlers = List.of(new SyncAlertMessageHandler(TGPlatform.getInstance(), this, registry));

        start();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isConnected() {
        return enabled && manager.isConnected();
    }

    public @Nullable StatefulRedisConnection<byte[], byte[]> connection() {
        return manager.connection();
    }

    public @Nullable RedisCommands<byte[], byte[]> sync() {
        StatefulRedisConnection<byte[], byte[]> currentConnection = connection();
        return (currentConnection != null && currentConnection.isOpen()) ? currentConnection.sync() : null;
    }

    public @Nullable StatefulRedisPubSubConnection<byte[], byte[]> pubSubConnection() {
        return manager.pubSubConnection();
    }

    public boolean shouldSendAlerts() {
        RedisOptions currentOptions = this.options;
        return enabled
                && currentOptions != null
                && currentOptions.getMessaging().hasChannel()
                && currentOptions.getMessaging().isSendAlerts();
    }

    public boolean shouldReceiveAlerts() {
        RedisOptions currentOptions = this.options;
        return enabled
                && currentOptions != null
                && currentOptions.getMessaging().hasChannel()
                && currentOptions.getMessaging().isReceiveAlerts();
    }

    public <T> boolean publish(Packet<T> packet, T payload) {
        RedisBroker currentBroker = this.broker;
        return currentBroker != null && currentBroker.publish(packet, payload);
    }

    @Blocking
    public synchronized void start() {
        applyOptions(new RedisOptions(), false);
    }

    @Blocking
    public synchronized void stop() {
        enabled = false;
        options = null;
        reloadHandlers();
        stopBroker();
        manager.stop();
    }

    @Blocking
    public synchronized void restart() {
        applyOptions(new RedisOptions(), true);
    }

    private void applyOptions(RedisOptions newOptions, boolean restartConnections) {
        this.options = newOptions;
        this.enabled = newOptions.isEnabled();

        reloadHandlers();
        stopBroker();

        if (!enabled) {
            manager.stop();
            return;
        }

        if (restartConnections) {
            manager.restart(newOptions);
        } else {
            manager.start(newOptions);
        }

        RedisBroker newBroker = new RedisBroker(registry, identifier, newOptions.getMessaging());
        newBroker.start(connection(), pubSubConnection());
        this.broker = newBroker;
    }

    private void reloadHandlers() {
        handlers.forEach(Reloadable::reload);
    }

    private void stopBroker() {
        RedisBroker currentBroker = this.broker;
        this.broker = null;

        if (currentBroker != null) {
            currentBroker.stop();
        }
    }
}
