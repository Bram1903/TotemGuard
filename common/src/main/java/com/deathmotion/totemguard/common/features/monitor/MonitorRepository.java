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

package com.deathmotion.totemguard.common.features.monitor;

import com.deathmotion.totemguard.api.event.impl.TGMonitorOpenEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.event.api.impl.TGMonitorOpenEventImpl;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryChangedEvent;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.PresenceListener;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncMonitorSubscribePacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncMonitorUnsubscribePacket;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class MonitorRepository implements PresenceListener, ConnectionStateListener {

    private static final long PUBLISH_INTERVAL_MILLIS = 500L;

    private final TGPlatform platform;

    private final ConcurrentHashMap<UUID, Set<UUID>> hostSubscribers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicInteger> viewerRefcount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, MonitorSnapshot> latestSnapshot = new ConcurrentHashMap<>();
    // Last snapshot we actually published per target. Lets the heartbeat tick skip the
    // publish + N-fanout cost when nothing material changed since the previous tick.
    private final ConcurrentHashMap<UUID, MonitorSnapshot> lastPublishedSnapshot = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private @Nullable ScheduledTask publishTask;
    private @Nullable AutoCloseable inventoryEventSubscription;

    public MonitorRepository(TGPlatform platform) {
        this.platform = platform;
    }

    private static void cancel(@Nullable ScheduledTask task) {
        if (task != null) task.cancel();
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;

        publishTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::publishHostUpdates,
                PUBLISH_INTERVAL_MILLIS, PUBLISH_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

        inventoryEventSubscription = platform.getEventRepository().subscribeInternal(
                InventoryChangedEvent.class,
                event -> {
                    UUID uuid = event.getPlayer().getUuid();
                    platform.getScheduler().runAsyncTask(() -> publishForTargetIfSubscribed(uuid));
                });

        platform.getRedisRepository().addStateListener(this);
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) return;
        cancel(publishTask);
        publishTask = null;

        if (inventoryEventSubscription != null) {
            try {
                inventoryEventSubscription.close();
            } catch (Exception ignored) {
            }
            inventoryEventSubscription = null;
        }

        platform.getRedisRepository().removeStateListener(this);

        UUID viewerInstance = platform.getNetworkPresenceRepository().identity().instanceId();
        for (UUID target : List.copyOf(viewerRefcount.keySet())) {
            publishUnsubscribe(viewerInstance, target);
        }

        hostSubscribers.clear();
        viewerRefcount.clear();
        latestSnapshot.clear();
        lastPublishedSnapshot.clear();
    }

    public void openLocalMonitor(@NotNull UUID targetUuid) {
        AtomicInteger count = viewerRefcount.computeIfAbsent(targetUuid, ignored -> new AtomicInteger());
        int now = count.incrementAndGet();
        if (now == 1) {
            publishSubscribe(targetUuid);
        }
    }

    public void closeLocalMonitor(@NotNull UUID targetUuid) {
        AtomicInteger count = viewerRefcount.get(targetUuid);
        if (count == null) return;
        int remaining = count.decrementAndGet();
        if (remaining > 0) return;
        viewerRefcount.remove(targetUuid, count);
        latestSnapshot.remove(targetUuid);
        UUID viewerInstance = platform.getNetworkPresenceRepository().identity().instanceId();
        publishUnsubscribe(viewerInstance, targetUuid);
    }

    public @Nullable MonitorSnapshot lastSnapshot(@NotNull UUID targetUuid) {
        return latestSnapshot.get(targetUuid);
    }

    public void acceptUpdate(@NotNull MonitorSnapshot snapshot) {
        UUID target = snapshot.targetUuid();
        if (!viewerRefcount.containsKey(target)) return;
        latestSnapshot.put(target, snapshot);
        if (platform.getGuiManager() != null) {
            platform.getGuiManager().refreshMonitor(target);
        }
    }

    public void acceptSubscribe(@NotNull SyncMonitorSubscribePacket.Payload payload) {
        if (platform.getPlayerRepository().getPlayer(payload.targetUuid()) == null) return;

        Set<UUID> subs = hostSubscribers.computeIfAbsent(
                payload.targetUuid(), ignored -> ConcurrentHashMap.newKeySet());
        subs.add(payload.viewerInstanceId());
    }

    public void acceptUnsubscribe(@NotNull SyncMonitorUnsubscribePacket.Payload payload) {
        Set<UUID> subs = hostSubscribers.get(payload.targetUuid());
        if (subs == null) return;
        subs.remove(payload.viewerInstanceId());
        if (subs.isEmpty()) {
            hostSubscribers.remove(payload.targetUuid(), subs);
            lastPublishedSnapshot.remove(payload.targetUuid());
        }
    }

    private void publishHostUpdates() {
        if (hostSubscribers.isEmpty()) return;
        if (!platform.getRedisRepository().isEnabled()) return;
        for (UUID target : hostSubscribers.keySet()) {
            publishForTargetIfSubscribed(target);
        }
    }

    private void publishForTargetIfSubscribed(UUID target) {
        Set<UUID> subs = hostSubscribers.get(target);
        if (subs == null || subs.isEmpty()) return;
        TGPlayer local = platform.getPlayerRepository().getPlayer(target);
        if (local == null) {
            hostSubscribers.remove(target);
            lastPublishedSnapshot.remove(target);
            return;
        }
        try {
            MonitorSnapshot snapshot = MonitorSnapshot.captureFrom(
                    local, platform.getNetworkPresenceRepository().getLocalServerName());

            MonitorSnapshot previous = lastPublishedSnapshot.get(target);
            if (previous != null && snapshot.contentEquals(previous)) return;
            lastPublishedSnapshot.put(target, snapshot);

            // Unicast: send to each viewer's server only, not the whole fleet.
            for (UUID viewerInstance : subs) {
                platform.getRedisRepository().publishToInstance(
                        viewerInstance, Packets.SYNC_MONITOR_UPDATE.packet(), snapshot);
            }
        } catch (Exception ex) {
            platform.getLogger().log(Level.WARNING, "Failed to capture monitor snapshot for " + target, ex);
        }
    }

    private void publishSubscribe(UUID target) {
        if (!platform.getRedisRepository().isEnabled()) return;
        UUID viewerInstance = platform.getNetworkPresenceRepository().identity().instanceId();
        platform.getRedisRepository().publish(
                Packets.SYNC_MONITOR_SUBSCRIBE.packet(),
                new SyncMonitorSubscribePacket.Payload(viewerInstance, target));
    }

    private void publishUnsubscribe(UUID viewerInstance, UUID target) {
        if (!platform.getRedisRepository().isEnabled()) return;
        platform.getRedisRepository().publish(
                Packets.SYNC_MONITOR_UNSUBSCRIBE.packet(),
                new SyncMonitorUnsubscribePacket.Payload(viewerInstance, target));
    }

    @Override
    public void onPlayerOffline(UUID playerUuid, RemotePlayerEntry lastKnown) {
        hostSubscribers.remove(playerUuid);
        latestSnapshot.remove(playerUuid);
        lastPublishedSnapshot.remove(playerUuid);
        GuiManager guiManager = platform.getGuiManager();
        if (guiManager == null) return;

        Set<UUID> viewers = guiManager.monitorViewers(playerUuid);
        if (!viewers.isEmpty()) {
            String label = lastKnown.playerName().isEmpty() ? playerUuid.toString() : lastKnown.playerName();
            for (UUID viewerId : viewers) {
                PlatformPlayer viewer = platform.getPlatformPlayerFactory().create(viewerId);
                if (viewer == null) continue;
                viewer.sendMessage(platform.getMessageService().getComponent(
                        MessagesKeys.MONITOR_TARGET_OFFLINE,
                        Map.of("tg_player", label)
                ));
            }
        }
        guiManager.closeMonitor(playerUuid);
    }

    @Override
    public void onServerOffline(UUID instanceId) {
        Iterator<Map.Entry<UUID, Set<UUID>>> it = hostSubscribers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Set<UUID>> entry = it.next();
            entry.getValue().remove(instanceId);
            if (entry.getValue().isEmpty()) {
                it.remove();
                lastPublishedSnapshot.remove(entry.getKey());
            }
        }
    }

    @Override
    public void onConnected(RedisConnection connection) {
        if (viewerRefcount.isEmpty()) return;
        platform.getScheduler().runAsyncTask(() -> {
            NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
            for (UUID target : List.copyOf(viewerRefcount.keySet())) {
                RemotePlayerEntry entry = presence == null ? null : presence.findByUuid(target);
                if (entry == null) {
                    onPlayerOffline(target, new RemotePlayerEntry(target, "", new UUID(0L, 0L), ""));
                    continue;
                }
                publishSubscribe(target);
            }
        });
    }

    @Override
    public void onPlayerOnline(UUID playerUuid, RemotePlayerEntry entry) {
        if (!viewerRefcount.containsKey(playerUuid)) return;

        GuiManager gui = platform.getGuiManager();
        Set<UUID> viewers = gui != null ? gui.monitorViewers(playerUuid) : Set.of();
        if (!viewers.isEmpty()) {
            UUID localInstance = platform.getNetworkPresenceRepository().identity().instanceId();
            boolean crossServer = !localInstance.equals(entry.serverInstanceId());
            TGPlayer localTarget = crossServer ? null : platform.getPlayerRepository().getPlayer(playerUuid);
            String proxyServerId = platform.resolveProxyServerId(entry.serverName());
            for (UUID viewerId : viewers) {
                TGMonitorOpenEvent event = platform.getEventRepository().post(
                        new TGMonitorOpenEventImpl(
                                viewerId, playerUuid, entry.playerName(), localTarget,
                                entry.serverInstanceId(), entry.serverName(),
                                proxyServerId, crossServer, true)
                );
                if (event.isCancelled()) gui.close(viewerId, true);
            }
        }

        publishSubscribe(playerUuid);
    }
}
