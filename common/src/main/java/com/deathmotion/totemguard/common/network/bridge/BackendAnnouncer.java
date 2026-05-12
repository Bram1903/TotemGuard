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
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.deathmotion.totemguard.proxybridge.protocol.v1.BridgeProtocol;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class BackendAnnouncer implements ConnectionStateListener {

    private static final long ANNOUNCE_PERIOD_SECONDS = 30L;

    private final TGPlatform platform;
    private final UUID instanceId;
    private final String displayName;
    private final byte[] channel;

    private volatile @Nullable ScheduledTask heartbeat;
    private volatile @Nullable List<String> cachedAddresses;
    private volatile int cachedPort;

    public BackendAnnouncer(@NotNull TGPlatform platform, @NotNull UUID instanceId, @NotNull String displayName) {
        this.platform = platform;
        this.instanceId = instanceId;
        this.displayName = displayName;
        this.channel = BridgeProtocol.CHANNEL_BACKEND_EVENTS.getBytes(StandardCharsets.UTF_8);
    }

    private static List<String> localInterfaceAddresses() {
        List<String> out = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a.isLinkLocalAddress() || a.isMulticastAddress()) continue;
                    out.add(a.getHostAddress());
                }
            }
        } catch (SocketException ignored) {
        }
        return out;
    }

    @Override
    public void onConnected(RedisConnection conn) {
        refreshCandidates();
        publishHello();
        scheduleHeartbeat();
    }

    @Override
    public void onDisconnected() {
        cancelHeartbeat();
    }

    public void stop() {
        cancelHeartbeat();
        publishGoodbyeBlocking();
    }

    private void scheduleHeartbeat() {
        cancelHeartbeat();
        this.heartbeat = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::publishHello,
                ANNOUNCE_PERIOD_SECONDS, ANNOUNCE_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void cancelHeartbeat() {
        ScheduledTask t = this.heartbeat;
        this.heartbeat = null;
        if (t != null) t.cancel();
    }

    private void refreshCandidates() {
        int port = platform.getServerPort();
        Set<String> addresses = new LinkedHashSet<>();
        String hint = platform.getProxyFacingHost();
        if (hint != null && !hint.isEmpty()) {
            addresses.add(hint);
        } else {
            addresses.add("127.0.0.1");
            addresses.add("localhost");
            addresses.addAll(localInterfaceAddresses());
        }
        this.cachedPort = port;
        this.cachedAddresses = List.copyOf(addresses);
    }

    private void publishHello() {
        List<String> addresses = this.cachedAddresses;
        if (addresses == null) return;
        String packed = String.join(String.valueOf(BridgeProtocol.LIST), addresses);
        publish(BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_HELLO,
                instanceId.toString(), displayName, Integer.toString(cachedPort), packed));
    }

    private void publishGoodbyeBlocking() {
        try {
            RedisConnection conn = platform.getRedisRepository().connection();
            if (conn == null || !conn.isOpen()) return;
            byte[] payload = BridgeProtocol.encode(BridgeProtocol.EV_BACKEND_GOODBYE, instanceId.toString())
                    .getBytes(StandardCharsets.UTF_8);
            conn.commands().sync().publish(channel, payload);
        } catch (Exception ex) {
            platform.getLogger().log(Level.FINE, "BackendAnnouncer goodbye failed: " + ex.getMessage());
        }
    }

    private void publish(String payload) {
        try {
            RedisConnection conn = platform.getRedisRepository().connection();
            if (conn == null || !conn.isOpen()) return;
            conn.commands().async().publish(channel, payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            platform.getLogger().log(Level.FINE, "BackendAnnouncer publish failed: " + ex.getMessage());
        }
    }
}
