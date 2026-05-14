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

import com.deathmotion.totemguard.api.redis.RedisRepository;
import com.deathmotion.totemguard.api.reload.Reloadable;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.schema.RedisOptions;
import com.deathmotion.totemguard.common.redis.broker.MessagingTopic;
import com.deathmotion.totemguard.common.redis.broker.RedisBroker;
import com.deathmotion.totemguard.common.redis.broker.handlers.*;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.PacketRegistry;
import org.jetbrains.annotations.NotNull;
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

    public RedisRepositoryImpl(@NotNull UUID instanceId) {
        this.manager = new RedisConnectionManager();
        this.registry = new PacketRegistry();
        // Use the shared ServerIdentity instance id so the broker's own-message rejection
        // and the per-instance unicast channel suffix line up with what publishers use
        // (e.g. MonitorRepository.publishSubscribe sends presence.identity().instanceId()).
        this.identifier = instanceId.toString();
        this.handlers = List.of(
                new SyncAlertMessageHandler(TGPlatform.getInstance(), this, registry),
                new SyncFocusAlertHandler(TGPlatform.getInstance(), this, registry),
                new SyncUpdateAvailableHandler(TGPlatform.getInstance(), this, registry),
                new SyncServerOfflineHandler(TGPlatform.getInstance(), this, registry),
                new SyncPlayerJoinHandler(TGPlatform.getInstance(), this, registry),
                new SyncPlayerOfflineHandler(TGPlatform.getInstance(), this, registry),
                new SyncTeleportRequestHandler(TGPlatform.getInstance(), this, registry),
                new SyncMonitorSubscribeHandler(TGPlatform.getInstance(), this, registry),
                new SyncMonitorUnsubscribeHandler(TGPlatform.getInstance(), this, registry),
                new SyncMonitorUpdateHandler(TGPlatform.getInstance(), this, registry),
                new SyncCheckRequestHandler(TGPlatform.getInstance(), this, registry),
                new SyncCheckResultHandler(TGPlatform.getInstance(), this, registry),
                new SyncFollowEventHandler(TGPlatform.getInstance(), this, registry)
        );

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

    public boolean isClusterMode() {
        RedisOptions current = this.options;
        return current != null && current.enabled() && current.cluster();
    }

    public boolean shouldSend(MessagingTopic topic) {
        RedisOptions current = this.options;
        if (current == null || !current.enabled() || !current.cluster()) return false;
        return switch (topic) {
            case ALERTS -> current.messaging().alerts().send();
            case FOCUS, UPDATES, PRESENCE -> true;
        };
    }

    public boolean shouldReceive(MessagingTopic topic) {
        RedisOptions current = this.options;
        if (current == null || !current.enabled() || !current.cluster()) return false;
        return switch (topic) {
            case ALERTS -> current.messaging().alerts().receive();
            case FOCUS, UPDATES, PRESENCE -> true;
        };
    }

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

    public <T> CompletionStage<Boolean> publishToInstance(UUID targetInstance, Packet<T> packet, T payload) {
        RedisBroker current = this.broker;
        if (current == null) return CompletableFuture.completedFuture(false);
        return current.publishToInstance(targetInstance, packet, payload);
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
        RedisOptions previous = this.options;
        boolean wasCluster = previous != null && previous.enabled() && previous.cluster();
        boolean willBeCluster = newOptions.enabled() && newOptions.cluster();
        if (wasCluster && !willBeCluster) {
            announceClusterLeaving();
        }

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

        if (newOptions.cluster()) {
            RedisBroker newBroker = new RedisBroker(registry, identifier, newOptions.messaging());
            manager.addListener(newBroker);
            this.broker = newBroker;
        }

        manager.start(newOptions, blocking);
    }

    private void reloadHandlers() {
        handlers.forEach(Reloadable::reload);
    }

    private void announceClusterLeaving() {
        try {
            TGPlatform tg = TGPlatform.getInstance();
            if (tg == null) return;
            com.deathmotion.totemguard.common.network.NetworkPresenceRepository presence = tg.getNetworkPresenceRepository();
            if (presence != null) {
                try {
                    presence.purgeClusterFootprint();
                } catch (Exception ex) {
                    TGPlatform.getInstance().getLogger().warning(
                            "Failed to purge presence footprint while leaving cluster: " + ex.getMessage());
                }
            }
            com.deathmotion.totemguard.common.network.bridge.BackendAnnouncer announcer = tg.getBackendAnnouncer();
            if (announcer != null) {
                try {
                    announcer.announceClusterLeaving();
                } catch (Exception ex) {
                    TGPlatform.getInstance().getLogger().warning(
                            "Failed to publish bridge goodbye while leaving cluster: " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            // TGPlatform may not be fully initialized yet (e.g. first applyOptions during construction).
        }
    }

    private void teardownBroker() {
        RedisBroker current = this.broker;
        this.broker = null;
        if (current == null) return;
        manager.removeListener(current);
    }
}
