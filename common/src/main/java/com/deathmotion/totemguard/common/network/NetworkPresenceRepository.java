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

import com.deathmotion.totemguard.api.network.NetworkRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncPlayerJoinPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncPlayerQuitPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncServerOfflinePacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NetworkPresenceRepository implements NetworkRepository {

    private static final long HEARTBEAT_PERIOD_SECONDS = 10L;
    private static final long SWEEP_PERIOD_SECONDS = 30L;
    private static final long OFFLINE_GRACE_MILLIS = 5_000L;
    private static final int SUGGEST_LIMIT = 50;

    private final TGPlatform platform;
    private final Logger logger;
    private final ServerIdentity identity;
    private final PresenceStore store;
    private final List<PresenceListener> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<UUID, ScheduledTask> pendingOffline = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile String effectiveDisplayName;
    private @Nullable ScheduledTask heartbeatTask;
    private @Nullable ScheduledTask sweepTask;

    public NetworkPresenceRepository(TGPlatform platform, ServerIdentity identity) {
        this.platform = platform;
        this.logger = platform.getLogger();
        this.identity = identity;
        this.store = new PresenceStore(platform.getRedisRepository(), logger);
        this.effectiveDisplayName = identity.displayName();
    }

    public void updateEffectiveDisplayName(@NotNull String newName) {
        if (newName.equals(effectiveDisplayName)) return;
        effectiveDisplayName = newName;

        Set<UUID> owned = new HashSet<>();
        for (TGPlayer player : platform.getPlayerRepository().getPlayers()) {
            owned.add(player.getUuid());
        }
        platform.getScheduler().runAsyncTask(() -> {
            store.updateHostServerName(identity.instanceId(), newName, owned);
            publishHeartbeat();
        });
    }

    public ServerIdentity identity() {
        return identity;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;
        heartbeatTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::publishHeartbeat, 1L, HEARTBEAT_PERIOD_SECONDS, TimeUnit.SECONDS);
        sweepTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::sweepStaleServers, SWEEP_PERIOD_SECONDS, SWEEP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) return;
        if (heartbeatTask != null) heartbeatTask.cancel();
        if (sweepTask != null) sweepTask.cancel();
        heartbeatTask = null;
        sweepTask = null;

        for (ScheduledTask task : pendingOffline.values()) {
            task.cancel();
        }
        pendingOffline.clear();

        store.purgeServer(identity.instanceId());
        publishServerOffline();
    }

    public void addListener(PresenceListener listener) {
        listeners.add(listener);
    }

    public void onLocalPlayerJoin(@NotNull UUID playerUuid, @NotNull String playerName, @Nullable UserProfile profile) {
        cancelPendingOffline(playerUuid);

        String name = effectiveDisplayName;
        notifyPlayerOnline(playerUuid, new RemotePlayerEntry(playerUuid, playerName, identity.instanceId(), name));
        platform.getScheduler().runAsyncTask(() -> {
            store.addPlayer(playerUuid, playerName, identity.instanceId(), name, profile);
            if (!platform.getRedisRepository().isEnabled()) return;
            Packet<SyncPlayerJoinPacket.Payload> packet = Packets.SYNC_PLAYER_JOIN.packet();
            platform.getRedisRepository().publish(packet, new SyncPlayerJoinPacket.Payload(
                    identity.instanceId(), name, playerUuid, playerName));
        });
    }

    public @Nullable UserProfile loadProfile(@NotNull UUID playerUuid) {
        return store.loadProfile(playerUuid);
    }

    public void onLocalPlayerQuit(@NotNull UUID playerUuid, @NotNull String playerName) {
        scheduleDeferredOffline(playerUuid, playerName, identity.instanceId(), effectiveDisplayName);

        platform.getScheduler().runAsyncTask(() -> {
            store.removePlayer(playerUuid, playerName, identity.instanceId());
            if (!platform.getRedisRepository().isEnabled()) return;
            Packet<SyncPlayerQuitPacket.Payload> packet = Packets.SYNC_PLAYER_QUIT.packet();
            platform.getRedisRepository().publish(packet, new SyncPlayerQuitPacket.Payload(
                    identity.instanceId(), playerUuid, playerName));
        });
    }

    public @Nullable RemotePlayerEntry findByUuid(@NotNull UUID uuid) {
        TGPlayer local = platform.getPlayerRepository().getPlayer(uuid);
        if (local != null) {
            String name = local.getName();
            if (name != null) {
                return new RemotePlayerEntry(uuid, name, identity.instanceId(), effectiveDisplayName);
            }
        }
        return store.findByUuid(uuid);
    }

    public @Nullable RemotePlayerEntry findByName(@NotNull String name) {
        for (TGPlayer local : platform.getPlayerRepository().getPlayers()) {
            if (name.equalsIgnoreCase(local.getName())) {
                return new RemotePlayerEntry(local.getUuid(), local.getName(),
                        identity.instanceId(), effectiveDisplayName);
            }
        }
        return store.findByName(name);
    }

    public @NotNull List<String> suggestNames(@NotNull String prefix) {
        if (!platform.getRedisRepository().isConnected()) {
            return localNamesMatching(prefix);
        }
        List<String> network = store.suggestNames(prefix, SUGGEST_LIMIT);
        if (network.isEmpty()) {
            return localNamesMatching(prefix);
        }
        return network;
    }

    public boolean isLocal(@NotNull UUID serverInstance) {
        return identity.instanceId().equals(serverInstance);
    }

    public int fleetSize() {
        return getConnectedServerCount();
    }

    public int trackedPlayerCount() {
        return getTrackedPlayerCount();
    }

    @Override
    public int getConnectedServerCount() {
        if (!platform.getRedisRepository().isConnected()) return 1;
        return Math.max(1, store.countServers());
    }

    @Override
    public int getTrackedPlayerCount() {
        if (!platform.getRedisRepository().isConnected()) {
            return platform.getPlayerRepository().getPlayers().size();
        }
        return store.trackedPlayerCount();
    }

    @Override
    public @NotNull String getLocalServerName() {
        return effectiveDisplayName;
    }

    @Override
    public boolean isConnected() {
        return platform.getRedisRepository().isConnected();
    }

    public void acceptServerOffline(@NotNull SyncServerOfflinePacket.Payload payload) {
        if (isLocal(payload.instanceId())) return;
        platform.getScheduler().runAsyncTask(() -> store.purgeServer(payload.instanceId()));
    }

    public void acceptPlayerJoin(@NotNull SyncPlayerJoinPacket.Payload payload) {
        if (isLocal(payload.instanceId())) return;
        cancelPendingOffline(payload.playerUuid());
        notifyPlayerOnline(payload.playerUuid(), new RemotePlayerEntry(
                payload.playerUuid(), payload.playerName(), payload.instanceId(), payload.serverName()));
    }

    public void acceptPlayerQuit(@NotNull SyncPlayerQuitPacket.Payload payload) {
        if (isLocal(payload.instanceId())) return;
        if (platform.getPlayerRepository().getPlayer(payload.playerUuid()) != null) return;
        platform.getScheduler().runAsyncTask(() -> {
            RemotePlayerEntry current = store.findByUuid(payload.playerUuid());
            if (current != null && !current.serverInstanceId().equals(payload.instanceId())) return;
            scheduleDeferredOffline(payload.playerUuid(), payload.playerName(), payload.instanceId(), "");
        });
    }

    public void acceptTeleportRequest(@NotNull SyncTeleportRequestPacket.Payload payload) {
        platform.handleIncomingTeleportRequest(payload);
    }

    public void publishTeleportRequest(@NotNull SyncTeleportRequestPacket.Payload payload) {
        if (!platform.getRedisRepository().isEnabled()) return;
        Packet<SyncTeleportRequestPacket.Payload> packet = Packets.SYNC_TELEPORT_REQUEST.packet();
        platform.getRedisRepository().publish(packet, payload);
    }

    private void publishHeartbeat() {
        try {
            Set<UUID> ownedPlayers = new HashSet<>();
            for (TGPlayer player : platform.getPlayerRepository().getPlayers()) {
                if (player.getName() == null) continue;
                ownedPlayers.add(player.getUuid());
            }
            store.heartbeatHost(identity.instanceId(), effectiveDisplayName, ownedPlayers);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "TotemGuard heartbeat failed", ex);
        }
    }

    private void publishServerOffline() {
        if (!platform.getRedisRepository().isEnabled()) return;
        Packet<SyncServerOfflinePacket.Payload> packet = Packets.SYNC_SERVER_OFFLINE.packet();
        platform.getRedisRepository().publish(packet, new SyncServerOfflinePacket.Payload(
                identity.instanceId(), effectiveDisplayName));
    }

    private void sweepStaleServers() {
        if (!platform.getRedisRepository().isConnected()) return;
        store.sweepStaleServers(identity.instanceId());
    }

    private void scheduleDeferredOffline(UUID playerUuid, String playerName, UUID iid, String serverName) {
        RemotePlayerEntry lastKnown = new RemotePlayerEntry(playerUuid, playerName, iid, serverName);
        Runnable fire = () -> {
            pendingOffline.remove(playerUuid);
            if (platform.getPlayerRepository().getPlayer(playerUuid) != null) return;
            if (store.findByUuid(playerUuid) != null) return;
            for (PresenceListener listener : listeners) {
                try {
                    listener.onPlayerOffline(playerUuid, lastKnown);
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Presence listener threw on offline", ex);
                }
            }
        };
        ScheduledTask task = platform.getScheduler().runAsyncTaskDelayed(
                fire, OFFLINE_GRACE_MILLIS, TimeUnit.MILLISECONDS);
        ScheduledTask previous = pendingOffline.put(playerUuid, task);
        if (previous != null) previous.cancel();
    }

    private void cancelPendingOffline(UUID playerUuid) {
        ScheduledTask task = pendingOffline.remove(playerUuid);
        if (task != null) task.cancel();
    }

    private void notifyPlayerOnline(UUID playerUuid, RemotePlayerEntry entry) {
        for (PresenceListener listener : listeners) {
            try {
                listener.onPlayerOnline(playerUuid, entry);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Presence listener threw on online", ex);
            }
        }
    }

    private List<String> localNamesMatching(String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (TGPlayer player : platform.getPlayerRepository().getPlayers()) {
            String name = player.getName();
            if (name == null) continue;
            if (lower.isEmpty() || name.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(name);
            }
        }
        return out;
    }
}
