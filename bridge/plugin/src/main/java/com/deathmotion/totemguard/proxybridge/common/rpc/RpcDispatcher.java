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

package com.deathmotion.totemguard.proxybridge.common.rpc;

import com.deathmotion.totemguard.proxybridge.common.BridgePlatform;
import com.deathmotion.totemguard.proxybridge.common.redis.BridgeRedis;
import com.deathmotion.totemguard.proxybridge.common.state.BackendDirectory;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

public final class RpcDispatcher {

    private final BridgeRedis redis;
    private final BridgePlatform platform;
    private final BackendDirectory directory;
    private final Listener listener = new Listener();

    public RpcDispatcher(@NotNull BridgeRedis redis,
                         @NotNull BridgePlatform platform,
                         @NotNull BackendDirectory directory) {
        this.redis = redis;
        this.platform = platform;
        this.directory = directory;
    }

    public void start() {
        StatefulRedisPubSubConnection<String, String> ps = redis.pubsub();
        if (ps == null) return;
        ps.addListener(listener);
        ps.sync().subscribe(BridgeProtocol.CHANNEL_RPC);
    }

    public void stop() {
        StatefulRedisPubSubConnection<String, String> ps = redis.pubsub();
        if (ps == null) return;
        try {
            ps.removeListener(listener);
        } catch (Exception ignored) {
        }
    }

    private void dispatch(@NotNull String message) {
        String[] parts = BridgeProtocol.decode(message);
        if (parts == null || parts.length == 0) return;
        try {
            if (BridgeProtocol.RPC_CONNECT.equals(parts[0])) handleConnect(parts);
        } catch (Exception ex) {
            platform.logger().log(Level.WARNING, "RPC dispatch failed: " + ex.getMessage());
        }
    }

    private void handleConnect(@NotNull String[] parts) {
        if (parts.length < 4) return;
        UUID playerUuid;
        UUID targetInstance;
        try {
            playerUuid = UUID.fromString(parts[2]);
            targetInstance = UUID.fromString(parts[3]);
        } catch (IllegalArgumentException ex) {
            return;
        }
        String slot = directory.slotForInstance(targetInstance);
        if (slot == null) return;
        platform.connect(playerUuid, slot);
    }

    private final class Listener extends RedisPubSubAdapter<String, String> {
        @Override
        public void message(String channel, String message) {
            dispatch(message);
        }
    }
}
