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

package com.deathmotion.totemguard.proxybridge.common.state;

import com.deathmotion.totemguard.proxybridge.common.BridgePlatform;
import com.deathmotion.totemguard.proxybridge.common.ProxyIdentity;
import com.deathmotion.totemguard.proxybridge.common.redis.BridgeRedis;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BackendDirectory {

    private final BridgeRedis redis;
    private final BridgePlatform platform;
    private final ProxyIdentity identity;
    private final Logger logger;
    private final Listener listener = new Listener();
    private final ConcurrentHashMap<String, UUID> slotToInstance = new ConcurrentHashMap<>();

    public BackendDirectory(@NotNull BridgeRedis redis,
                            @NotNull BridgePlatform platform,
                            @NotNull ProxyIdentity identity) {
        this.redis = redis;
        this.platform = platform;
        this.identity = identity;
        this.logger = platform.logger();
    }

    public void start() {
        StatefulRedisPubSubConnection<String, String> ps = redis.pubsub();
        if (ps == null) return;
        try {
            ps.removeListener(listener);
        } catch (Exception ignored) {
        }
        ps.addListener(listener);
        ps.sync().subscribe(BridgeProtocol.CHANNEL_EVENTS);
    }

    public void stop() {
        StatefulRedisPubSubConnection<String, String> ps = redis.pubsub();
        if (ps == null) return;
        try {
            ps.removeListener(listener);
        } catch (Exception ignored) {
        }
    }

    public @Nullable String slotForInstance(@NotNull UUID instanceId) {
        for (Map.Entry<String, UUID> entry : slotToInstance.entrySet()) {
            if (entry.getValue().equals(instanceId)) return entry.getKey();
        }
        return null;
    }

    public @Nullable UUID instanceForSlot(@NotNull String slot) {
        return slotToInstance.get(slot);
    }

    public synchronized void revaluate() {
        Set<String> configured = platform.registeredBackendNames();
        for (String slot : Set.copyOf(slotToInstance.keySet())) {
            if (!configured.contains(slot)) {
                UUID dropped = slotToInstance.remove(slot);
                if (dropped != null) {
                    logger.info("Disconnected slot \"" + slot + "\" (removed from proxy config).");
                }
            }
        }
        StatefulRedisConnection<String, String> conn = redis.connection();
        if (conn == null) return;
        for (Map.Entry<String, UUID> entry : Map.copyOf(slotToInstance).entrySet()) {
            String slot = entry.getKey();
            String key = BridgeProtocol.keyProxySlot(identity.id(), slot);
            try {
                Long exists = conn.sync().exists(key);
                if (exists != null && exists == 0L) {
                    UUID dropped = slotToInstance.remove(slot);
                    if (dropped != null) {
                        logger.info("Disconnected slot \"" + slot + "\" (backend offline).");
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.FINE, "BackendDirectory liveness check failed: " + ex.getMessage());
            }
        }
    }

    private void onBound(UUID proxyId, String slot, UUID instanceId) {
        if (!proxyId.equals(identity.id())) return;
        if (!platform.registeredBackendNames().contains(slot)) return;
        UUID prev = slotToInstance.put(slot, instanceId);
        if (prev == null || !prev.equals(instanceId)) {
            logger.info("Connected slot \"" + slot + "\".");
        }
    }

    private void onUnbound(UUID proxyId, String slot, UUID instanceId) {
        if (!proxyId.equals(identity.id())) return;
        UUID current = slotToInstance.get(slot);
        if (current != null && current.equals(instanceId)) {
            slotToInstance.remove(slot);
            logger.info("Disconnected slot \"" + slot + "\".");
        }
    }

    private final class Listener extends RedisPubSubAdapter<String, String> {
        @Override
        public void message(String channel, String message) {
            if (!BridgeProtocol.CHANNEL_EVENTS.equals(channel)) return;
            String[] parts = BridgeProtocol.decode(message);
            if (parts == null || parts.length == 0) return;
            try {
                switch (parts[0]) {
                    case BridgeProtocol.EV_BACKEND_BOUND -> {
                        if (parts.length < 4) return;
                        UUID proxyId = parseUuid(parts[1]);
                        String slot = parts[2];
                        UUID instanceId = parseUuid(parts[3]);
                        if (proxyId == null || instanceId == null) return;
                        onBound(proxyId, slot, instanceId);
                    }
                    case BridgeProtocol.EV_BACKEND_UNBOUND -> {
                        if (parts.length < 4) return;
                        UUID proxyId = parseUuid(parts[1]);
                        String slot = parts[2];
                        UUID instanceId = parseUuid(parts[3]);
                        if (proxyId == null || instanceId == null) return;
                        onUnbound(proxyId, slot, instanceId);
                    }
                    default -> {
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "BackendDirectory dispatch failed: " + ex.getMessage());
            }
        }

        private @Nullable UUID parseUuid(String value) {
            try {
                return UUID.fromString(value);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
