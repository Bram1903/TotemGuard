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

package com.deathmotion.totemguard.common.network.bridge;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.ProxyTopologyService;
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProxyBridgeSubscriber extends RedisPubSubAdapter<byte[], byte[]>
        implements ConnectionStateListener {

    private static final byte[] EVENTS_CHANNEL = BridgeProtocol.CHANNEL_EVENTS.getBytes(StandardCharsets.UTF_8);

    private final TGPlatform platform;
    private final Logger logger;

    public ProxyBridgeSubscriber(TGPlatform platform) {
        this.platform = platform;
        this.logger = platform.getLogger();
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String readString(java.util.Map<byte[], byte[]> hash, String key) {
        byte[] needle = key.getBytes(StandardCharsets.UTF_8);
        for (java.util.Map.Entry<byte[], byte[]> e : hash.entrySet()) {
            if (java.util.Arrays.equals(e.getKey(), needle)) {
                return new String(e.getValue(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public void onConnected(RedisConnection conn) {
        StatefulRedisPubSubConnection<byte[], byte[]> pubSub = conn.pubSub();
        try {
            pubSub.removeListener(this);
        } catch (Exception ignored) {
        }
        pubSub.addListener(this);
        pubSub.async().subscribe(EVENTS_CHANNEL).whenComplete((ignored, error) -> {
            if (error != null) {
                logger.log(Level.WARNING, "Failed to subscribe to proxy events: " + error.getMessage());
            }
        });
        bootstrapTopology(conn);
    }

    @Override
    public void onDisconnected() {
        ProxyTopologyService topology = platform.getProxyTopologyService();
        if (topology != null) topology.clear();
    }

    @Override
    public void message(byte[] channel, byte[] message) {
        if (channel == null || message == null) return;
        if (!java.util.Arrays.equals(channel, EVENTS_CHANNEL)) return;
        try {
            String payload = new String(message, StandardCharsets.UTF_8);
            dispatch(payload);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to handle proxy event", ex);
        }
    }

    private void dispatch(String payload) {
        String[] parts = BridgeProtocol.decode(payload);
        if (parts == null || parts.length == 0) return;
        ProxyTopologyService topology = platform.getProxyTopologyService();
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (topology == null) return;

        switch (parts[0]) {
            case BridgeProtocol.EV_PROXY_ONLINE -> {
                if (parts.length < 4) return;
                UUID proxyId = parseUuid(parts[1]);
                if (proxyId == null) return;
                topology.acceptProxy(proxyId, parts[2], parts[3], Set.of());
            }
            case BridgeProtocol.EV_PROXY_OFFLINE -> {
                if (parts.length < 2) return;
                UUID proxyId = parseUuid(parts[1]);
                if (proxyId == null) return;
                topology.forgetProxy(proxyId);
            }
            case BridgeProtocol.EV_BACKEND_ADDED -> {
                if (parts.length < 3) return;
                UUID proxyId = parseUuid(parts[1]);
                if (proxyId == null) return;
                topology.addBackend(proxyId, parts[2]);
            }
            case BridgeProtocol.EV_BACKEND_BOUND -> {
                if (parts.length < 4) return;
                UUID proxyId = parseUuid(parts[1]);
                String slot = parts[2];
                UUID instanceId = parseUuid(parts[3]);
                if (proxyId == null || instanceId == null) return;
                topology.recordSlotInstance(proxyId, slot, instanceId);
                if (presence == null) return;
                if (!instanceId.equals(presence.identity().instanceId())) return;
                if (topology.localProxyId() == null) {
                    topology.bindLocalProxy(proxyId);
                }
            }
            case BridgeProtocol.EV_BACKEND_REMOVED -> {
                if (parts.length < 3) return;
                UUID proxyId = parseUuid(parts[1]);
                if (proxyId == null) return;
                topology.removeBackend(proxyId, parts[2]);
            }
            case BridgeProtocol.EV_PLAYER_JOIN -> {
                if (presence == null || parts.length < 3) return;
                UUID playerUuid = parseUuid(parts[2]);
                if (playerUuid == null) return;
                presence.onProxyPlayerJoin(playerUuid);
            }
            case BridgeProtocol.EV_PLAYER_SWITCH -> {
                if (presence == null || parts.length < 3) return;
                UUID playerUuid = parseUuid(parts[2]);
                if (playerUuid == null) return;
                presence.onProxyPlayerSwitch(playerUuid);
            }
            case BridgeProtocol.EV_PLAYER_QUIT -> {
                if (presence == null || parts.length < 3) return;
                UUID playerUuid = parseUuid(parts[2]);
                if (playerUuid == null) return;
                presence.onProxyPlayerQuit(playerUuid);
            }
            default -> {
            }
        }
    }

    private void bootstrapTopology(RedisConnection conn) {
        byte[] registryKey = BridgeProtocol.KEY_REGISTRY.getBytes(StandardCharsets.UTF_8);
        conn.commands().async().smembers(registryKey).whenComplete((ids, error) -> {
            if (error != null || ids == null) return;
            for (byte[] raw : ids) {
                String idStr = new String(raw, StandardCharsets.UTF_8);
                UUID proxyId = parseUuid(idStr);
                if (proxyId != null) loadProxy(conn, proxyId);
            }
        });
    }

    private void loadProxy(RedisConnection conn, UUID proxyId) {
        byte[] proxyKey = BridgeProtocol.keyProxy(proxyId).getBytes(StandardCharsets.UTF_8);
        byte[] backendsKey = BridgeProtocol.keyProxyBackends(proxyId).getBytes(StandardCharsets.UTF_8);
        byte[] instancesKey = BridgeProtocol.keyProxyInstances(proxyId).getBytes(StandardCharsets.UTF_8);
        byte[] registryKey = BridgeProtocol.KEY_REGISTRY.getBytes(StandardCharsets.UTF_8);
        conn.commands().async().hgetall(proxyKey).whenComplete((hash, hashErr) -> {
            if (hashErr != null) return;
            if (hash == null || hash.isEmpty()) {
                conn.commands().async().srem(registryKey, proxyId.toString().getBytes(StandardCharsets.UTF_8));
                return;
            }
            String displayName = readString(hash, BridgeProtocol.HASH_DISPLAY_NAME);
            String platformName = readString(hash, BridgeProtocol.HASH_PLATFORM);
            conn.commands().async().smembers(backendsKey).whenComplete((backends, beErr) -> {
                if (beErr != null) return;
                Set<String> names = new LinkedHashSet<>();
                if (backends != null) {
                    for (byte[] b : backends) names.add(new String(b, StandardCharsets.UTF_8));
                }
                ProxyTopologyService topology = platform.getProxyTopologyService();
                if (topology == null) return;
                topology.acceptProxy(proxyId,
                        displayName == null ? "" : displayName,
                        platformName == null ? "" : platformName,
                        "",
                        names);
                conn.commands().async().hgetall(instancesKey).whenComplete((instances, inErr) -> {
                    if (inErr != null) return;
                    checkLocalBindingByInstance(topology, proxyId, instances);
                });
            });
        });
    }

    private void checkLocalBindingByInstance(ProxyTopologyService topology, UUID proxyId,
                                             java.util.Map<byte[], byte[]> instances) {
        if (instances == null || instances.isEmpty()) return;
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        UUID self = presence == null ? null : presence.identity().instanceId();
        for (java.util.Map.Entry<byte[], byte[]> entry : instances.entrySet()) {
            String slot = new String(entry.getKey(), StandardCharsets.UTF_8);
            UUID candidate;
            try {
                candidate = UUID.fromString(new String(entry.getValue(), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            topology.recordSlotInstance(proxyId, slot, candidate);
            if (self != null && candidate.equals(self) && topology.localProxyId() == null) {
                topology.bindLocalProxy(proxyId);
            }
        }
    }
}
