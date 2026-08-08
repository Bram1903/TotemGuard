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
import com.deathmotion.totemguard.common.network.PresenceListener;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.redis.RedisRepositoryImpl;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class BridgeManager implements PresenceListener {

    private final TGPlatform platform;
    private final ProxyBridgeSubscriber subscriber;
    @Getter
    private final BridgeBindingHeartbeat heartbeat;
    @Getter
    private final BridgeHandshakeListener handshakeListener;

    public BridgeManager(@NotNull TGPlatform platform, @NotNull UUID instanceId) {
        this.platform = platform;
        this.subscriber = new ProxyBridgeSubscriber(platform);
        this.heartbeat = new BridgeBindingHeartbeat(platform, instanceId);
        this.handshakeListener = new BridgeHandshakeListener(platform, heartbeat);
    }

    public void start() {
        RedisRepositoryImpl redis = platform.getRedisRepository();
        if (redis == null) return;
        redis.addStateListener(subscriber);
        redis.addStateListener(heartbeat);
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence != null) presence.addListener(this);
    }

    public void shutdown() {
        RedisRepositoryImpl redis = platform.getRedisRepository();
        if (redis != null) {
            redis.removeStateListener(heartbeat);
            redis.removeStateListener(subscriber);
        }
        try {
            heartbeat.stop();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPlayerOffline(@NotNull UUID playerUuid, @NotNull RemotePlayerEntry lastKnown) {
    }

    @Override
    public void onLocalPlayerQuit(@NotNull UUID playerUuid) {
        handshakeListener.forget(playerUuid);
    }
}
