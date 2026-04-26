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
import com.deathmotion.totemguard.common.config.schema.RedisOptions;
import com.deathmotion.totemguard.common.redis.broker.RedisBroker;
import com.deathmotion.totemguard.common.redis.broker.handlers.SyncAlertMessageHandler;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class RedisRepositoryImpl implements RedisRepository {

    private final RedisConnectionManager manager;
    private final PacketRegistry registry;
    private final List<Reloadable> handlers;
    private final String identifier;

    private volatile @Nullable RedisOptions options;
    private volatile @Nullable RedisBroker broker;

    public RedisRepositoryImpl() {
        this.manager = new RedisConnectionManager();
        this.registry = new PacketRegistry();
        this.identifier = UUID.randomUUID().toString();
        this.handlers = List.of(new SyncAlertMessageHandler(TGPlatform.getInstance(), this, registry));

        // Initial start is blocking so the connection is up before any player can join.
        applyOptions(currentOptions(), false, true);
    }

    @Override
    public boolean isEnabled() {
        RedisOptions current = this.options;
        return current != null && current.enabled();
    }

    @Override
    public boolean isConnected() {
        return isEnabled() && manager.isConnected();
    }

    public boolean shouldSendAlerts() {
        RedisOptions current = this.options;
        return isEnabled()
                && current.messaging().hasChannel()
                && current.messaging().sendAlerts();
    }

    public boolean shouldReceiveAlerts() {
        RedisOptions current = this.options;
        return isEnabled()
                && current.messaging().hasChannel()
                && current.messaging().receiveAlerts();
    }

    // Ephemeral — re-fetch per operation, do not cache.
    public @Nullable RedisConnection connection() {
        return isEnabled() ? manager.connection() : null;
    }

    public void addStateListener(ConnectionStateListener listener) {
        manager.addListener(listener);
    }

    public void removeStateListener(ConnectionStateListener listener) {
        manager.removeListener(listener);
    }

    public <T> CompletionStage<Boolean> publish(Packet<T> packet, T payload) {
        RedisBroker current = this.broker;
        if (current == null) return CompletableFuture.completedFuture(false);
        return current.publish(packet, payload);
    }

    public synchronized void start() {
        applyOptions(currentOptions(), false, false);
    }

    public synchronized void stop() {
        options = null;
        reloadHandlers();
        teardownBroker();
        manager.stop();
    }

    public synchronized void restart() {
        applyOptions(currentOptions(), true, false);
    }

    private RedisOptions currentOptions() {
        return TGPlatform.getInstance().getConfigRepository().configView().redis();
    }

    private void applyOptions(RedisOptions newOptions, boolean restart, boolean blocking) {
        this.options = newOptions;

        reloadHandlers();
        teardownBroker();

        // Stop before wiring the new broker so addListener doesn't fire onConnected with a doomed conn.
        if (restart || !newOptions.enabled()) {
            manager.stop();
        }

        if (!newOptions.enabled()) {
            return;
        }

        RedisBroker newBroker = new RedisBroker(registry, identifier, newOptions.messaging());
        if (newBroker.isConfigured()) {
            manager.addListener(newBroker);
        }
        this.broker = newBroker;

        manager.start(newOptions, blocking);
    }

    private void reloadHandlers() {
        handlers.forEach(Reloadable::reload);
    }

    private void teardownBroker() {
        RedisBroker current = this.broker;
        this.broker = null;
        if (current == null) return;
        manager.removeListener(current);
    }
}
