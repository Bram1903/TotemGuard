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
import com.deathmotion.totemguard.common.network.ProxyTopologyService;
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.SetArgs;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class BridgeBindingHeartbeat implements ConnectionStateListener {

    private static final long HEARTBEAT_PERIOD_SECONDS = 30L;
    private static final long BINDING_TTL_SECONDS = 90L;
    private static final byte[] CHANNEL_EVENTS =
            BridgeProtocol.CHANNEL_EVENTS.getBytes(StandardCharsets.UTF_8);

    private final TGPlatform platform;
    private final UUID instanceId;
    private final ConcurrentHashMap<UUID, ActiveBinding> activeBindings = new ConcurrentHashMap<>();

    private volatile @Nullable ScheduledTask heartbeatTask;

    public BridgeBindingHeartbeat(@NotNull TGPlatform platform, @NotNull UUID instanceId) {
        this.platform = platform;
        this.instanceId = instanceId;
    }

    @Override
    public void onConnected(RedisConnection conn) {
        if (!platform.getRedisRepository().isClusterMode()) return;
        republishAll(conn);
        startHeartbeat();
    }

    @Override
    public void onDisconnected() {
        cancelHeartbeat();
    }

    public void acceptHandshake(@NotNull UUID proxyId, @NotNull String slot, @NotNull String displayName) {
        ActiveBinding existing = activeBindings.get(proxyId);
        boolean unchanged = existing != null
                && existing.slot().equals(slot)
                && existing.displayName().equals(displayName);
        if (unchanged) return;
        ActiveBinding next = new ActiveBinding(slot, displayName, System.currentTimeMillis());
        activeBindings.put(proxyId, next);

        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn != null && conn.isOpen()) {
            writeBinding(conn, proxyId, next, true);
        }

        ProxyTopologyService topology = platform.getProxyTopologyService();
        if (topology != null) {
            topology.bindLocalProxy(proxyId, displayName);
        }
    }

    public void onProxyOffline(@NotNull UUID proxyId) {
        ActiveBinding removed = activeBindings.remove(proxyId);
        if (removed == null) return;
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn != null && conn.isOpen()) {
            deleteBindingKeys(conn, proxyId, removed.slot());
        }
    }

    public void stop() {
        cancelHeartbeat();
        Map<UUID, ActiveBinding> snapshot = Map.copyOf(activeBindings);
        activeBindings.clear();
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn == null || !conn.isOpen()) return;
        for (Map.Entry<UUID, ActiveBinding> e : snapshot.entrySet()) {
            UUID proxyId = e.getKey();
            ActiveBinding b = e.getValue();
            deleteBindingKeys(conn, proxyId, b.slot());
            publishUnboundBlocking(conn, proxyId, b.slot());
        }
    }

    private void startHeartbeat() {
        cancelHeartbeat();
        this.heartbeatTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::heartbeat,
                HEARTBEAT_PERIOD_SECONDS, HEARTBEAT_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelHeartbeat() {
        ScheduledTask t = this.heartbeatTask;
        this.heartbeatTask = null;
        if (t != null) t.cancel();
    }

    private void heartbeat() {
        if (activeBindings.isEmpty()) return;
        if (!platform.getRedisRepository().isClusterMode()) return;
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn == null || !conn.isOpen()) return;

        for (Map.Entry<UUID, ActiveBinding> e : activeBindings.entrySet()) {
            UUID proxyId = e.getKey();
            ActiveBinding b = e.getValue();
            byte[] proxyKey = BridgeProtocol.keyProxy(proxyId).getBytes(StandardCharsets.UTF_8);
            conn.commands().async().exists(proxyKey).whenComplete((count, error) -> {
                if (error != null) {
                    platform.getLogger().log(Level.FINE,
                            "BridgeBindingHeartbeat existence check failed: " + error.getMessage());
                    return;
                }
                if (count == null || count == 0L) {
                    ActiveBinding removed = activeBindings.remove(proxyId);
                    if (removed != null) {
                        RedisConnection live = platform.getRedisRepository().connection();
                        if (live != null && live.isOpen()) {
                            deleteBindingKeys(live, proxyId, removed.slot());
                        }
                        ProxyTopologyService topology = platform.getProxyTopologyService();
                        if (topology != null) topology.unbindIf(proxyId);
                    }
                    return;
                }
                try {
                    refreshBindingTtl(conn, proxyId, b);
                } catch (Exception ex) {
                    platform.getLogger().log(Level.FINE,
                            "BridgeBindingHeartbeat refresh failed: " + ex.getMessage());
                }
            });
        }
    }

    private void republishAll(RedisConnection conn) {
        for (Map.Entry<UUID, ActiveBinding> e : activeBindings.entrySet()) {
            writeBinding(conn, e.getKey(), e.getValue(), true);
        }
    }

    private void writeBinding(RedisConnection conn, UUID proxyId, ActiveBinding b, boolean publishEvent) {
        byte[] slotKey = BridgeProtocol.keyProxySlot(proxyId, b.slot())
                .getBytes(StandardCharsets.UTF_8);
        byte[] instanceProxyKey = BridgeProtocol.keyInstanceProxy(instanceId)
                .getBytes(StandardCharsets.UTF_8);
        byte[] setKey = BridgeProtocol.keyProxyInstanceSet(proxyId)
                .getBytes(StandardCharsets.UTF_8);
        byte[] instanceIdBytes = instanceId.toString().getBytes(StandardCharsets.UTF_8);
        byte[] proxyIdBytes = proxyId.toString().getBytes(StandardCharsets.UTF_8);

        SetArgs args = SetArgs.Builder.ex(BINDING_TTL_SECONDS);
        conn.commands().async().set(slotKey, instanceIdBytes, args);
        conn.commands().async().set(instanceProxyKey, proxyIdBytes, args);
        conn.commands().async().sadd(setKey, instanceIdBytes);
        conn.commands().async().expire(setKey, BINDING_TTL_SECONDS);

        if (publishEvent) {
            byte[] payload = BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_BOUND,
                            proxyId.toString(), b.slot(), instanceId.toString())
                    .getBytes(StandardCharsets.UTF_8);
            conn.commands().async().publish(CHANNEL_EVENTS, payload);
        }
    }

    private void refreshBindingTtl(RedisConnection conn, UUID proxyId, ActiveBinding b) {
        byte[] slotKey = BridgeProtocol.keyProxySlot(proxyId, b.slot())
                .getBytes(StandardCharsets.UTF_8);
        byte[] instanceProxyKey = BridgeProtocol.keyInstanceProxy(instanceId)
                .getBytes(StandardCharsets.UTF_8);
        byte[] setKey = BridgeProtocol.keyProxyInstanceSet(proxyId)
                .getBytes(StandardCharsets.UTF_8);
        conn.commands().async().expire(slotKey, BINDING_TTL_SECONDS);
        conn.commands().async().expire(instanceProxyKey, BINDING_TTL_SECONDS);
        conn.commands().async().expire(setKey, BINDING_TTL_SECONDS);
    }

    private void deleteBindingKeys(RedisConnection conn, UUID proxyId, String slot) {
        byte[] slotKey = BridgeProtocol.keyProxySlot(proxyId, slot)
                .getBytes(StandardCharsets.UTF_8);
        byte[] instanceProxyKey = BridgeProtocol.keyInstanceProxy(instanceId)
                .getBytes(StandardCharsets.UTF_8);
        byte[] setKey = BridgeProtocol.keyProxyInstanceSet(proxyId)
                .getBytes(StandardCharsets.UTF_8);
        byte[] instanceIdBytes = instanceId.toString().getBytes(StandardCharsets.UTF_8);
        conn.commands().async().del(slotKey);
        conn.commands().async().del(instanceProxyKey);
        conn.commands().async().srem(setKey, instanceIdBytes);
    }

    private void publishUnboundBlocking(RedisConnection conn, UUID proxyId, String slot) {
        try {
            byte[] payload = BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_UNBOUND,
                            proxyId.toString(), slot, instanceId.toString())
                    .getBytes(StandardCharsets.UTF_8);
            conn.commands().sync().publish(CHANNEL_EVENTS, payload);
        } catch (Exception ex) {
            platform.getLogger().log(Level.FINE,
                    "BridgeBindingHeartbeat unbound publish failed: " + ex.getMessage());
        }
    }

    private record ActiveBinding(@NotNull String slot, @NotNull String displayName, long establishedAtMillis) {
    }
}
