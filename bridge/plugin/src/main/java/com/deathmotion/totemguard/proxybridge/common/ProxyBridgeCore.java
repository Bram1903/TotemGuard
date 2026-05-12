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

package com.deathmotion.totemguard.proxybridge.common;

import com.deathmotion.totemguard.proxybridge.common.log.BridgeLogSuppressor;
import com.deathmotion.totemguard.proxybridge.common.presence.PresencePublisher;
import com.deathmotion.totemguard.proxybridge.common.redis.BridgeRedis;
import com.deathmotion.totemguard.proxybridge.common.rpc.RpcDispatcher;
import com.deathmotion.totemguard.proxybridge.common.state.BackendDirectory;
import com.deathmotion.totemguard.proxybridge.common.state.BackendTracker;
import com.deathmotion.totemguard.proxybridge.common.state.ProxyStatePublisher;
import com.deathmotion.totemguard.proxybridge.common.state.StaleProxySweeper;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProxyBridgeCore {

    private static final long REFRESH_PERIOD_SECONDS = 30L;

    private final BridgePlatform platform;
    private final ProxyConfigHolder config;
    private final BridgeRedis redis;
    private final BackendTracker tracker;
    private final StaleProxySweeper sweeper;
    private final BackendDirectory backendDirectory;
    private final ProxyStatePublisher state;
    private final PresencePublisher presence;
    private final RpcDispatcher rpc;

    private final AtomicBoolean started = new AtomicBoolean();

    public ProxyBridgeCore(@NotNull BridgePlatform platform,
                           @NotNull ProxyConfig initialConfig,
                           @NotNull ProxyIdentity identity) {
        this.platform = platform;
        this.config = new ProxyConfigHolder(initialConfig);
        this.redis = new BridgeRedis(initialConfig.redis(), platform.logger());
        this.tracker = new BackendTracker();
        this.sweeper = new StaleProxySweeper(redis, platform.logger());
        this.backendDirectory = new BackendDirectory(redis, platform, identity);
        this.state = new ProxyStatePublisher(redis, identity, config, platform, tracker, backendDirectory);
        this.presence = new PresencePublisher(redis, identity, platform.logger());
        this.rpc = new RpcDispatcher(redis, platform, backendDirectory);
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;

        BridgeLogSuppressor.suppressDefaultNoise();

        redis.start();
        rpc.start();
        backendDirectory.start();
        sweeper.sweep();
        state.republish(true);

        platform.scheduleRepeating(() -> {
            backendDirectory.revaluate();
            state.republish(false);
        }, REFRESH_PERIOD_SECONDS, REFRESH_PERIOD_SECONDS, TimeUnit.SECONDS);

        platform.logger().info("Online (" + platform.kind()
                + ", " + tracker.size() + " backends).");
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) return;
        state.publishOffline();
        backendDirectory.stop();
        rpc.stop();
        redis.stop();
    }

    public void refreshBackends() {
        backendDirectory.revaluate();
        state.publishBackendDiff();
    }

    public synchronized void reload(@NotNull ProxyConfig newConfig) {
        if (!started.get()) {
            config.replace(newConfig);
            return;
        }
        ProxyConfig.Redis previousRedis = config.current().redis();
        config.replace(newConfig);
        backendDirectory.stop();
        rpc.stop();
        if (!previousRedis.equals(newConfig.redis())) {
            redis.restart(newConfig.redis());
        }
        rpc.start();
        backendDirectory.start();
        backendDirectory.revaluate();
        sweeper.sweep();
        state.republish(true);
        platform.logger().info("Configuration reloaded.");
    }

    public void onPlayerJoin(@NotNull UUID playerUuid) {
        presence.onJoin(playerUuid);
    }

    public void onPlayerSwitch(@NotNull UUID playerUuid) {
        presence.onSwitch(playerUuid);
    }

    public void onPlayerQuit(@NotNull UUID playerUuid) {
        presence.onQuit(playerUuid);
    }
}
