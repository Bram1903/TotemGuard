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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BackendDirectory {

    private static final long BACKEND_TTL_MILLIS = 90_000L;

    private final BridgeRedis redis;
    private final BridgePlatform platform;
    private final ProxyIdentity identity;
    private final Logger logger;
    private final Listener listener = new Listener();
    private final ConcurrentHashMap<UUID, KnownBackend> knownBackends = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> slotToInstance = new ConcurrentHashMap<>();

    public BackendDirectory(@NotNull BridgeRedis redis,
                            @NotNull BridgePlatform platform,
                            @NotNull ProxyIdentity identity) {
        this.redis = redis;
        this.platform = platform;
        this.identity = identity;
        this.logger = platform.logger();
    }

    private static Set<InetAddress> resolve(String host) {
        if (host == null || host.isEmpty()) return Collections.emptySet();
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            Set<InetAddress> out = new LinkedHashSet<>(addrs.length);
            Collections.addAll(out, addrs);
            return out;
        } catch (UnknownHostException ex) {
            return Collections.emptySet();
        }
    }

    private static Set<String> parseAddresses(String packed) {
        if (packed == null || packed.isEmpty()) return Collections.emptySet();
        String[] parts = packed.split(String.valueOf(BridgeProtocol.LIST));
        Set<String> out = new LinkedHashSet<>(parts.length);
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    public void start() {
        StatefulRedisPubSubConnection<String, String> ps = redis.pubsub();
        if (ps == null) return;
        try {
            ps.removeListener(listener);
        } catch (Exception ignored) {
        }
        ps.addListener(listener);
        ps.sync().subscribe(BridgeProtocol.CHANNEL_BACKEND_EVENTS);
    }

    public void stop() {
        StatefulRedisPubSubConnection<String, String> ps = redis.pubsub();
        if (ps == null) return;
        try {
            ps.removeListener(listener);
        } catch (Exception ignored) {
        }
    }

    public @NotNull Map<String, UUID> currentMappings() {
        return Map.copyOf(slotToInstance);
    }

    public @org.jetbrains.annotations.Nullable String slotForInstance(@NotNull UUID instanceId) {
        for (Map.Entry<String, UUID> entry : slotToInstance.entrySet()) {
            if (entry.getValue().equals(instanceId)) return entry.getKey();
        }
        return null;
    }

    public synchronized void revaluate() {
        Map<String, InetSocketAddress> slots = platform.registeredBackends();

        long now = System.currentTimeMillis();
        Map.copyOf(knownBackends).forEach((id, kb) -> {
            if (now - kb.lastSeenMillis > BACKEND_TTL_MILLIS) knownBackends.remove(id);
        });

        for (String slot : Set.copyOf(slotToInstance.keySet())) {
            if (!slots.containsKey(slot)) {
                if (slotToInstance.remove(slot) != null) {
                    logger.info("Disconnected slot \"" + slot + "\" (removed from proxy config).");
                }
            }
        }
        for (Map.Entry<String, UUID> entry : Map.copyOf(slotToInstance).entrySet()) {
            if (!knownBackends.containsKey(entry.getValue())) {
                slotToInstance.remove(entry.getKey());
                logger.info("Disconnected slot \"" + entry.getKey() + "\" (backend offline).");
            }
        }

        for (Map.Entry<String, InetSocketAddress> slot : slots.entrySet()) {
            if (slotToInstance.containsKey(slot.getKey())) continue;
            UUID matched = findMatchingBackend(slot.getValue());
            if (matched == null) continue;
            slotToInstance.put(slot.getKey(), matched);
            logger.info("Connected slot \"" + slot.getKey() + "\".");
            publishBound(slot.getKey(), matched);
        }
    }

    private @org.jetbrains.annotations.Nullable UUID findMatchingBackend(InetSocketAddress slot) {
        Set<InetAddress> slotIps = resolve(slot.getHostString());
        if (slotIps.isEmpty()) return null;
        int slotPort = slot.getPort();
        for (Map.Entry<UUID, KnownBackend> entry : knownBackends.entrySet()) {
            KnownBackend kb = entry.getValue();
            if (kb.port != slotPort) continue;
            for (String candidate : kb.addresses) {
                Set<InetAddress> candidateIps = resolve(candidate);
                for (InetAddress ip : candidateIps) {
                    if (slotIps.contains(ip)) return entry.getKey();
                }
            }
        }
        return null;
    }

    private void publishBound(String slot, UUID instanceId) {
        try {
            StatefulRedisConnection<String, String> conn = redis.connection();
            if (conn == null) return;
            conn.async().publish(BridgeProtocol.CHANNEL_EVENTS,
                    BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_BOUND,
                            identity.id().toString(), slot, instanceId.toString()));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to publish backend_bound: " + ex.getMessage());
        }
    }

    private void onHello(UUID instanceId, String displayName, int port, Set<String> addresses) {
        knownBackends.put(instanceId, new KnownBackend(displayName, port, addresses, System.currentTimeMillis()));
        revaluate();
    }

    private void onGoodbye(UUID instanceId) {
        knownBackends.remove(instanceId);
        revaluate();
    }

    private record KnownBackend(String displayName, int port, Set<String> addresses, long lastSeenMillis) {
    }

    private final class Listener extends RedisPubSubAdapter<String, String> {
        @Override
        public void message(String channel, String message) {
            String[] parts = BridgeProtocol.decode(message);
            if (parts == null || parts.length == 0) return;
            try {
                switch (parts[0]) {
                    case BridgeProtocol.EV_BACKEND_HELLO -> {
                        if (parts.length < 5) return;
                        UUID id;
                        int port;
                        try {
                            id = UUID.fromString(parts[1]);
                            port = Integer.parseInt(parts[3]);
                        } catch (IllegalArgumentException ex) {
                            return;
                        }
                        onHello(id, parts[2], port, parseAddresses(parts[4]));
                    }
                    case BridgeProtocol.EV_BACKEND_GOODBYE -> {
                        if (parts.length < 2) return;
                        UUID id;
                        try {
                            id = UUID.fromString(parts[1]);
                        } catch (IllegalArgumentException ex) {
                            return;
                        }
                        onGoodbye(id);
                    }
                    default -> {
                    }
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "BackendDirectory dispatch failed: " + ex.getMessage());
            }
        }
    }
}
