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

package com.deathmotion.totemguard.common.network;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.api.sync.RedisCommands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ProxyTopologyService {

    private final TGPlatform platform;
    private volatile @Nullable UUID localProxyId;
    private volatile @Nullable String localProxyDisplayName;

    public ProxyTopologyService(@NotNull TGPlatform platform) {
        this.platform = platform;
    }

    public @Nullable String localProxyServerName() {
        return localProxyDisplayName;
    }

    public @Nullable UUID localProxyId() {
        return localProxyId;
    }

    public boolean bridgeAvailable() {
        return localProxyId != null;
    }

    public boolean canRouteToInstance(@NotNull UUID targetInstanceId) {
        UUID proxy = localProxyId;
        if (proxy == null) return false;
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn == null || !conn.isOpen()) return false;
        try {
            RedisCommands<byte[], byte[]> sync = conn.commands().sync();
            Boolean member = sync.sismember(
                    BridgeProtocol.keyProxyInstanceSet(proxy).getBytes(StandardCharsets.UTF_8),
                    targetInstanceId.toString().getBytes(StandardCharsets.UTF_8));
            return Boolean.TRUE.equals(member);
        } catch (Exception ex) {
            return false;
        }
    }

    public void connectToInstance(@NotNull UUID playerUuid, @NotNull UUID targetInstanceId) {
        if (!platform.getRedisRepository().isClusterMode()) return;
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn == null || !conn.isOpen()) return;
        String message = BridgeProtocol.encode(BridgeProtocol.RPC_CONNECT,
                UUID.randomUUID().toString(), playerUuid.toString(), targetInstanceId.toString());
        byte[] channel = BridgeProtocol.CHANNEL_RPC.getBytes(StandardCharsets.UTF_8);
        conn.commands().async().publish(channel, message.getBytes(StandardCharsets.UTF_8));
    }

    public void bindLocalProxy(@NotNull UUID proxyId, @Nullable String displayName) {
        UUID previous = this.localProxyId;
        if (proxyId.equals(previous) && displayName != null && displayName.equals(localProxyDisplayName)) return;
        this.localProxyId = proxyId;
        if (displayName != null && !displayName.isBlank()) {
            this.localProxyDisplayName = displayName;
        }
        if (!proxyId.equals(previous)) {
            platform.getLogger().info("Hooked into TotemGuard-Bridge.");
        }
    }

    public void unbindIf(@NotNull UUID proxyId) {
        if (!proxyId.equals(this.localProxyId)) return;
        this.localProxyId = null;
        String previousName = this.localProxyDisplayName;
        this.localProxyDisplayName = null;
        String name = previousName == null ? proxyId.toString() : previousName;
        platform.getLogger().info("Lost TotemGuard-Bridge link (" + name + " went offline).");
    }

    public void clear() {
        boolean wasBound = localProxyId != null;
        localProxyId = null;
        localProxyDisplayName = null;
        if (wasBound) {
            platform.getLogger().info("Disconnected from TotemGuard-Bridge.");
        }
    }
}
