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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProxyTopologyService {

    private final TGPlatform platform;
    private final ConcurrentHashMap<UUID, ProxyView> proxies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, UUID>> slotsByProxy = new ConcurrentHashMap<>();

    private volatile @Nullable UUID localProxyId;
    private volatile @Nullable UUID pendingHookedLog;

    public ProxyTopologyService(@NotNull TGPlatform platform) {
        this.platform = platform;
    }

    public @Nullable String localProxyServerName() {
        UUID id = localProxyId;
        if (id == null) return null;
        ProxyView view = proxies.get(id);
        return view == null ? null : view.displayName();
    }

    public boolean canRouteToInstance(@NotNull UUID targetInstanceId) {
        UUID id = localProxyId;
        if (id == null) return false;
        ConcurrentHashMap<String, UUID> slots = slotsByProxy.get(id);
        return slots != null && slots.containsValue(targetInstanceId);
    }

    public void connectToInstance(@NotNull UUID playerUuid, @NotNull UUID targetInstanceId) {
        if (proxies.isEmpty()) return;
        RedisConnection conn = platform.getRedisRepository().connection();
        if (conn == null || !conn.isOpen()) return;
        String message = BridgeProtocol.encode(BridgeProtocol.RPC_CONNECT,
                UUID.randomUUID().toString(), playerUuid.toString(), targetInstanceId.toString());
        byte[] channel = BridgeProtocol.CHANNEL_RPC.getBytes(StandardCharsets.UTF_8);
        conn.commands().async().publish(channel, message.getBytes(StandardCharsets.UTF_8));
    }

    public boolean bridgeAvailable() {
        return !proxies.isEmpty();
    }

    public void clear() {
        boolean wasHooked = localProxyId != null;
        proxies.clear();
        slotsByProxy.clear();
        localProxyId = null;
        pendingHookedLog = null;
        if (wasHooked) {
            platform.getLogger().info("Disconnected from TotemGuard-Bridge.");
        }
    }

    public void acceptProxy(@NotNull UUID proxyId,
                            @NotNull String displayName,
                            @NotNull String platformName,
                            @NotNull Set<String> backends) {
        acceptProxy(proxyId, displayName, platformName, "", backends);
    }

    public void acceptProxy(@NotNull UUID proxyId,
                            @NotNull String displayName,
                            @NotNull String platformName,
                            @NotNull String externalAddress,
                            @NotNull Set<String> backends) {
        proxies.compute(proxyId, (k, existing) -> {
            Set<String> merged = new LinkedHashSet<>();
            if (existing != null) merged.addAll(existing.backends());
            merged.addAll(backends);
            return new ProxyView(displayName, platformName, merged);
        });
        if (proxyId.equals(pendingHookedLog)
                && !displayName.isBlank() && !platformName.isBlank()) {
            pendingHookedLog = null;
            logHooked(displayName, platformName);
        }
    }

    public void addBackend(@NotNull UUID proxyId, @NotNull String backendName) {
        proxies.compute(proxyId, (k, existing) -> {
            ProxyView base = existing != null ? existing : new ProxyView("", "", new LinkedHashSet<>());
            Set<String> merged = new LinkedHashSet<>(base.backends());
            merged.add(backendName);
            return new ProxyView(base.displayName(), base.platformName(), merged);
        });
    }

    public void removeBackend(@NotNull UUID proxyId, @NotNull String backendName) {
        proxies.computeIfPresent(proxyId, (k, existing) -> {
            Set<String> merged = new LinkedHashSet<>(existing.backends());
            merged.removeIf(b -> b.equalsIgnoreCase(backendName));
            return new ProxyView(existing.displayName(), existing.platformName(), merged);
        });
        ConcurrentHashMap<String, UUID> slots = slotsByProxy.get(proxyId);
        if (slots != null) slots.remove(backendName);
    }

    public void recordSlotInstance(@NotNull UUID proxyId, @NotNull String slot, @NotNull UUID instanceId) {
        slotsByProxy.computeIfAbsent(proxyId, k -> new ConcurrentHashMap<>()).put(slot, instanceId);
    }

    public void forgetProxy(@NotNull UUID proxyId) {
        ProxyView view = proxies.remove(proxyId);
        slotsByProxy.remove(proxyId);
        if (proxyId.equals(pendingHookedLog)) pendingHookedLog = null;
        if (proxyId.equals(localProxyId)) {
            localProxyId = null;
            String name = view == null || view.displayName().isBlank() ? proxyId.toString() : view.displayName();
            platform.getLogger().info("Lost TotemGuard-Bridge link (" + name + " went offline).");
        }
    }

    public void bindLocalProxy(@NotNull UUID proxyId) {
        if (proxyId.equals(this.localProxyId)) return;
        this.localProxyId = proxyId;
        ProxyView view = proxies.get(proxyId);
        if (view != null && !view.displayName().isBlank() && !view.platformName().isBlank()) {
            logHooked(view.displayName(), view.platformName());
        } else {
            pendingHookedLog = proxyId;
        }
    }

    private void logHooked(@NotNull String displayName, @NotNull String platformName) {
        platform.getLogger().info("Hooked into TotemGuard-Bridge: " + displayName + " (" + platformName + ").");
    }

    public @Nullable UUID localProxyId() {
        return localProxyId;
    }

    private record ProxyView(@NotNull String displayName, @NotNull String platformName,
                             @NotNull Set<String> backends) {
    }
}
