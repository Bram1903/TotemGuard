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
import com.deathmotion.totemguard.proxybridge.common.ProxyConfigHolder;
import com.deathmotion.totemguard.proxybridge.common.ProxyIdentity;
import com.deathmotion.totemguard.proxybridge.common.redis.BridgeRedis;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class ProxyStatePublisher {

    public static final long STATE_TTL_SECONDS = 90L;

    private final BridgeRedis redis;
    private final ProxyIdentity identity;
    private final ProxyConfigHolder config;
    private final BridgePlatform platform;
    private final BackendTracker tracker;
    private final BackendDirectory backendDirectory;

    public ProxyStatePublisher(@NotNull BridgeRedis redis,
                               @NotNull ProxyIdentity identity,
                               @NotNull ProxyConfigHolder config,
                               @NotNull BridgePlatform platform,
                               @NotNull BackendTracker tracker,
                               @NotNull BackendDirectory backendDirectory) {
        this.redis = redis;
        this.identity = identity;
        this.config = config;
        this.platform = platform;
        this.tracker = tracker;
        this.backendDirectory = backendDirectory;
    }

    public void republish(boolean announce) {
        StatefulRedisConnection<String, String> conn = redis.connection();
        if (conn == null) return;

        UUID id = identity.id();
        String idStr = id.toString();
        String proxyKey = BridgeProtocol.keyProxy(id);
        String backendsKey = BridgeProtocol.keyProxyBackends(id);
        Set<String> backends = platform.registeredBackendNames();
        long now = System.currentTimeMillis();

        try {
            conn.setAutoFlushCommands(false);
            RedisAsyncCommands<String, String> async = conn.async();

            async.sadd(BridgeProtocol.KEY_REGISTRY, idStr);

            async.hset(proxyKey, BridgeProtocol.HASH_DISPLAY_NAME, config.current().displayName());
            async.hset(proxyKey, BridgeProtocol.HASH_PLATFORM, platform.kind().name());
            async.hset(proxyKey, BridgeProtocol.HASH_STARTED_AT, String.valueOf(now));
            async.hset(proxyKey, BridgeProtocol.HASH_UPDATED_AT, String.valueOf(now));
            async.expire(proxyKey, STATE_TTL_SECONDS);

            async.del(backendsKey);
            if (!backends.isEmpty()) {
                async.sadd(backendsKey, backends.toArray(String[]::new));
                async.expire(backendsKey, STATE_TTL_SECONDS);
            }

            String instancesKey = BridgeProtocol.keyProxyInstances(id);
            String instanceSetKey = BridgeProtocol.keyProxyInstanceSet(id);
            async.del(instancesKey);
            async.del(instanceSetKey);
            java.util.Map<String, UUID> mappings = backendDirectory.currentMappings();
            if (!mappings.isEmpty()) {
                java.util.Map<String, String> serialized = new java.util.LinkedHashMap<>(mappings.size());
                String[] instanceIds = new String[mappings.size()];
                int i = 0;
                for (java.util.Map.Entry<String, UUID> entry : mappings.entrySet()) {
                    String iid = entry.getValue().toString();
                    serialized.put(entry.getKey(), iid);
                    instanceIds[i++] = iid;
                    async.set(BridgeProtocol.keyInstanceProxy(entry.getValue()), idStr,
                            io.lettuce.core.SetArgs.Builder.ex(STATE_TTL_SECONDS));
                }
                async.hset(instancesKey, serialized);
                async.expire(instancesKey, STATE_TTL_SECONDS);
                async.sadd(instanceSetKey, instanceIds);
                async.expire(instanceSetKey, STATE_TTL_SECONDS);
            }

            if (announce) {
                async.publish(BridgeProtocol.CHANNEL_EVENTS,
                        BridgeProtocol.encode(BridgeProtocol.EV_PROXY_ONLINE,
                                idStr, config.current().displayName(), platform.kind().name()));
                for (String name : backends) {
                    async.publish(BridgeProtocol.CHANNEL_EVENTS,
                            BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_ADDED, idStr, name));
                }
            }

            conn.flushCommands();
        } catch (Exception ex) {
            platform.logger().log(Level.WARNING,
                    "republish failed (next tick will retry): " + ex.getMessage());
            return;
        } finally {
            conn.setAutoFlushCommands(true);
        }

        tracker.recordPublished(backends);
    }

    public void publishBackendDiff() {
        StatefulRedisConnection<String, String> conn = redis.connection();
        if (conn == null) return;

        UUID id = identity.id();
        Set<String> current = platform.registeredBackendNames();
        BackendTracker.Diff diff = tracker.diff(current);
        if (diff.isEmpty()) return;

        String backendsKey = BridgeProtocol.keyProxyBackends(id);
        try {
            conn.setAutoFlushCommands(false);
            RedisAsyncCommands<String, String> async = conn.async();

            if (!diff.added().isEmpty()) async.sadd(backendsKey, diff.added().toArray(String[]::new));
            if (!diff.removed().isEmpty()) async.srem(backendsKey, diff.removed().toArray(String[]::new));
            async.expire(backendsKey, STATE_TTL_SECONDS);

            for (String name : diff.added()) {
                async.publish(BridgeProtocol.CHANNEL_EVENTS,
                        BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_ADDED, id.toString(), name));
            }
            for (String name : diff.removed()) {
                async.publish(BridgeProtocol.CHANNEL_EVENTS,
                        BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_REMOVED, id.toString(), name));
            }

            conn.flushCommands();
        } catch (Exception ex) {
            platform.logger().log(Level.WARNING, "publishBackendDiff failed: " + ex.getMessage());
            return;
        } finally {
            conn.setAutoFlushCommands(true);
        }

        tracker.recordPublished(current);
    }

    public void publishOffline() {
        StatefulRedisConnection<String, String> conn = redis.connection();
        if (conn == null) return;
        UUID id = identity.id();
        try {
            RedisCommands<String, String> cmd = conn.sync();
            cmd.publish(BridgeProtocol.CHANNEL_EVENTS,
                    BridgeProtocol.encode(BridgeProtocol.EV_PROXY_OFFLINE, id.toString()));
            cmd.srem(BridgeProtocol.KEY_REGISTRY, id.toString());
            java.util.Map<String, UUID> mappings = backendDirectory.currentMappings();
            if (!mappings.isEmpty()) {
                String[] keys = new String[mappings.size()];
                int i = 0;
                for (UUID iid : mappings.values()) {
                    keys[i++] = BridgeProtocol.keyInstanceProxy(iid);
                }
                cmd.del(keys);
            }
            cmd.del(BridgeProtocol.keyProxy(id),
                    BridgeProtocol.keyProxyBackends(id),
                    BridgeProtocol.keyProxyInstances(id),
                    BridgeProtocol.keyProxyInstanceSet(id));
        } catch (Exception ex) {
            platform.logger().log(Level.WARNING, "publishOffline failed: " + ex.getMessage());
        }
    }
}
