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
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProxyBridgeSubscriber extends RedisPubSubAdapter<byte[], byte[]>
        implements ConnectionStateListener {

    private static final byte[] EVENTS_CHANNEL = BridgeProtocol.CHANNEL_EVENTS.getBytes(StandardCharsets.UTF_8);
    private static final long LIVENESS_SWEEP_PERIOD_SECONDS = 45L;

    private final TGPlatform platform;
    private final Logger logger;
    private volatile @Nullable ScheduledTask livenessTask;

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
        resolveLocalBinding(conn);
        startLivenessSweep();
    }

    @Override
    public void onDisconnected() {
        stopLivenessSweep();
        ProxyTopologyService topology = platform.getProxyTopologyService();
        if (topology != null) topology.clear();
    }

    private void startLivenessSweep() {
        stopLivenessSweep();
        this.livenessTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::sweepLocalBinding,
                LIVENESS_SWEEP_PERIOD_SECONDS, LIVENESS_SWEEP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void stopLivenessSweep() {
        ScheduledTask task = this.livenessTask;
        this.livenessTask = null;
        if (task != null) task.cancel();
    }

    private void sweepLocalBinding() {
        ProxyTopologyService topology = platform.getProxyTopologyService();
        if (topology == null) return;
        UUID bound = topology.localProxyId();
        if (bound == null) return;
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn == null || !conn.isOpen()) return;
        byte[] key = BridgeProtocol.keyProxy(bound).getBytes(StandardCharsets.UTF_8);
        conn.commands().async().exists(key).whenComplete((count, error) -> {
            if (error != null || count == null) return;
            if (count == 0L) topology.unbindIf(bound);
        });
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
            case BridgeProtocol.EV_PROXY_OFFLINE -> {
                if (parts.length < 2) return;
                UUID proxyId = parseUuid(parts[1]);
                if (proxyId == null) return;
                topology.unbindIf(proxyId);
            }
            case BridgeProtocol.EV_BACKEND_BOUND -> {
                if (parts.length < 4) return;
                UUID proxyId = parseUuid(parts[1]);
                UUID instanceId = parseUuid(parts[3]);
                if (proxyId == null || instanceId == null) return;
                if (presence == null) return;
                if (!instanceId.equals(presence.identity().instanceId())) return;
                resolveProxyDisplayNameThenBind(proxyId);
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
                UUID destination = parts.length >= 4 ? parseUuid(parts[3]) : null;
                presence.onProxyPlayerSwitch(playerUuid, destination);
            }
            case BridgeProtocol.EV_PLAYER_DISCONNECT -> {
                if (presence == null || parts.length < 3) return;
                UUID playerUuid = parseUuid(parts[2]);
                if (playerUuid == null) return;
                presence.onProxyPlayerDisconnect(playerUuid);
            }
            case BridgeProtocol.EV_PLAYER_TRANSFER -> {
                if (presence == null || parts.length < 3) return;
                UUID playerUuid = parseUuid(parts[2]);
                if (playerUuid == null) return;
                presence.onProxyPlayerTransfer(playerUuid);
            }
            default -> {
            }
        }
    }

    private void resolveLocalBinding(RedisConnection conn) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        ProxyTopologyService topology = platform.getProxyTopologyService();
        if (presence == null || topology == null) return;
        UUID self = presence.identity().instanceId();
        byte[] key = BridgeProtocol.keyInstanceProxy(self).getBytes(StandardCharsets.UTF_8);
        conn.commands().async().get(key).whenComplete((value, error) -> {
            if (error != null || value == null) return;
            UUID proxyId = parseUuid(new String(value, StandardCharsets.UTF_8));
            if (proxyId != null) resolveProxyDisplayNameThenBind(proxyId);
        });
    }

    private void resolveProxyDisplayNameThenBind(UUID proxyId) {
        ProxyTopologyService topology = platform.getProxyTopologyService();
        if (topology == null) return;
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn == null || !conn.isOpen()) {
            topology.bindLocalProxy(proxyId, null);
            return;
        }
        byte[] proxyKey = BridgeProtocol.keyProxy(proxyId).getBytes(StandardCharsets.UTF_8);
        conn.commands().async().hgetall(proxyKey).whenComplete((hash, error) -> {
            String displayName = (error == null && hash != null && !hash.isEmpty())
                    ? readString(hash, BridgeProtocol.HASH_DISPLAY_NAME)
                    : null;
            topology.bindLocalProxy(proxyId, displayName);
        });
    }
}
