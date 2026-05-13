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

import com.deathmotion.totemguard.api.config.ConfigFile;
import com.deathmotion.totemguard.api.network.NetworkRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.ConfigKeys;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.redis.ConnectionStateListener;
import com.deathmotion.totemguard.common.redis.RedisConnection;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncPlayerJoinPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncPlayerOfflinePacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncServerOfflinePacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NetworkPresenceRepository implements NetworkRepository, ConnectionStateListener {

    private static final long HEARTBEAT_PERIOD_SECONDS = 10L;
    private static final long SWEEP_PERIOD_SECONDS = 30L;
    private static final long PLACEHOLDER_RETRY_PERIOD_SECONDS = 10L;
    private static final int PLACEHOLDER_RETRY_MAX_ATTEMPTS = 30;
    private static final int SUGGEST_LIMIT = 50;
    private static final long DISCONNECT_HINT_TTL_MILLIS = 5_000L;
    private static final long TRANSFER_GRACE_MILLIS = 30_000L;

    private final TGPlatform platform;
    private final Logger logger;
    private final ServerIdentity identity;
    private final PresenceStore store;
    private final List<PresenceListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> serverNameResolvedListeners = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<UUID, PendingOffline> pendingOffline = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> disconnectHints = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private volatile String effectiveDisplayName;
    @Getter
    private volatile boolean serverNameResolved;
    private @Nullable ScheduledTask heartbeatTask;
    private @Nullable ScheduledTask sweepTask;
    private @Nullable ScheduledTask placeholderRetryTask;

    public NetworkPresenceRepository(TGPlatform platform, ServerIdentity identity) {
        this.platform = platform;
        this.logger = platform.getLogger();
        this.identity = identity;
        this.store = new PresenceStore(platform.getRedisRepository(), logger);

        Optional<String> resolved = resolveServerName();
        this.effectiveDisplayName = resolved.orElseGet(this::generatedFallback);
        this.serverNameResolved = resolved.isPresent();
    }

    public void addServerNameResolvedListener(@NotNull Consumer<String> listener) {
        serverNameResolvedListeners.add(listener);
        if (serverNameResolved) {
            try {
                listener.accept(effectiveDisplayName);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Server-name listener threw on initial fire", ex);
            }
        }
    }

    public void reloadServerName() {
        Optional<String> resolved = resolveServerName();
        if (resolved.isPresent()) {
            applyResolvedName(resolved.get());
            cancelPlaceholderRetry();
            return;
        }

        if (serverNameResolved) {
            serverNameResolved = false;
            updateEffectiveDisplayName(generatedFallback());
        }
        startPlaceholderRetryIfNeeded();
    }

    private void applyResolvedName(@NotNull String name) {
        boolean wasResolved = serverNameResolved;
        boolean nameChanged = !name.equals(effectiveDisplayName);
        serverNameResolved = true;
        if (nameChanged) updateEffectiveDisplayName(name);
        if (!wasResolved || nameChanged) notifyServerNameResolved(name);
    }

    private void notifyServerNameResolved(@NotNull String name) {
        for (Consumer<String> listener : serverNameResolvedListeners) {
            try {
                listener.accept(name);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Server-name listener failed", ex);
            }
        }
    }

    private void startPlaceholderRetryIfNeeded() {
        if (serverNameResolved || placeholderRetryTask != null) return;
        int[] attempts = {0};
        placeholderRetryTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                () -> tickPlaceholderRetry(attempts),
                PLACEHOLDER_RETRY_PERIOD_SECONDS, PLACEHOLDER_RETRY_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    private void tickPlaceholderRetry(int[] attempts) {
        if (serverNameResolved) {
            cancelPlaceholderRetry();
            return;
        }
        Optional<String> resolved = resolveServerName();
        if (resolved.isPresent()) {
            applyResolvedName(resolved.get());
            cancelPlaceholderRetry();
            logger.info("Server name placeholder \""
                    + platform.getConfigRepository().configView().server()
                    + "\" resolved to \"" + resolved.get() + "\".");
            return;
        }
        attempts[0]++;
        if (attempts[0] >= PLACEHOLDER_RETRY_MAX_ATTEMPTS) {
            cancelPlaceholderRetry();
            logger.warning("Server name placeholder \""
                    + platform.getConfigRepository().configView().server()
                    + "\" did not resolve after " + PLACEHOLDER_RETRY_MAX_ATTEMPTS
                    + " attempts; staying on fallback \"" + effectiveDisplayName
                    + "\". Run /tg reload once the providing plugin is ready to retry.");
        }
    }

    private void cancelPlaceholderRetry() {
        ScheduledTask task = placeholderRetryTask;
        placeholderRetryTask = null;
        if (task != null) task.cancel();
    }

    private Optional<String> resolveServerName() {
        String raw = platform.getConfigRepository().configView().server();
        if (raw == null || raw.isBlank()) return Optional.empty();
        if (raw.indexOf('%') < 0) return Optional.of(raw);
        try {
            String result = platform.getPlaceholderRepository().replace(raw, null, null, Map.of());
            if (result == null || result.isBlank() || result.equals(raw)) return Optional.empty();
            if (result.matches(".*%[A-Za-z0-9_]+%.*")) return Optional.empty();
            return Optional.of(result);
        } catch (Exception ex) {
            logger.warning("Failed to resolve %tg_server% placeholder \"" + raw + "\": " + ex.getMessage());
            return Optional.empty();
        }
    }

    private String generatedFallback() {
        return "tg-" + Long.toHexString(identity.instanceId().getLeastSignificantBits()).substring(0, 8);
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
        platform.getRedisRepository().addStateListener(this);
        if (!serverNameResolved) startPlaceholderRetryIfNeeded();
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) return;
        cancelPlaceholderRetry();
        platform.getRedisRepository().removeStateListener(this);
        if (heartbeatTask != null) heartbeatTask.cancel();
        if (sweepTask != null) sweepTask.cancel();
        heartbeatTask = null;
        sweepTask = null;

        for (PendingOffline pending : pendingOffline.values()) {
            pending.task().cancel();
        }
        pendingOffline.clear();
        disconnectHints.clear();

        Set<UUID> owned = new HashSet<>();
        for (TGPlayer player : platform.getPlayerRepository().getPlayers()) {
            owned.add(player.getUuid());
        }
        if (platform.getSessionViolationStore() != null && !owned.isEmpty()) {
            platform.getSessionViolationStore().purgeIfOwnedBy(identity.instanceId(), owned);
        }
        if (platform.getModSessionStore() != null && !owned.isEmpty()) {
            platform.getModSessionStore().purgeIfOwnedBy(identity.instanceId(), owned);
        }
        store.purgeServer(identity.instanceId(), effectiveDisplayName);
        publishServerOffline();
    }

    public void addListener(PresenceListener listener) {
        listeners.add(listener);
    }

    public void onLocalPlayerJoin(@NotNull UUID playerUuid, @NotNull String playerName, @Nullable UserProfile profile) {
        cancelPendingOffline(playerUuid);
        disconnectHints.remove(playerUuid);

        String name = effectiveDisplayName;
        notifyLocalPlayerJoin(playerUuid);
        notifyPlayerOnline(playerUuid, new RemotePlayerEntry(playerUuid, playerName, identity.instanceId(), name));
        platform.getScheduler().runAsyncTask(() -> {
            store.addPlayer(playerUuid, playerName, identity.instanceId(), name, profile);
            if (platform.getSessionViolationStore() != null) {
                platform.getSessionViolationStore().onPlayerJoin(playerUuid, playerName, name, identity.instanceId());
            }
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
        notifyLocalPlayerQuit(playerUuid);

        RemotePlayerEntry lastKnown = new RemotePlayerEntry(
                playerUuid, playerName, identity.instanceId(), effectiveDisplayName);

        if (!platform.getRedisRepository().isConnected()) {
            notifyPlayerOffline(playerUuid, lastKnown);
            return;
        }

        if (consumeFreshDisconnectHint(playerUuid)) {
            platform.getScheduler().runAsyncTask(() -> finalizeOffline(playerUuid, playerName, lastKnown));
            return;
        }

        long graceMillis = offlineGraceMillis();
        if (graceMillis <= 0L) {
            platform.getScheduler().runAsyncTask(() -> finalizeOffline(playerUuid, playerName, lastKnown));
            return;
        }

        schedulePendingOffline(playerUuid, playerName, lastKnown, graceMillis);
    }

    private void schedulePendingOffline(UUID playerUuid, String playerName,
                                        RemotePlayerEntry lastKnown, long graceMillis) {
        ScheduledTask task = platform.getScheduler().runAsyncTaskDelayed(
                () -> {
                    pendingOffline.remove(playerUuid);
                    finalizeOffline(playerUuid, playerName, lastKnown);
                },
                graceMillis, TimeUnit.MILLISECONDS);
        PendingOffline previous = pendingOffline.put(playerUuid,
                new PendingOffline(task, playerName, lastKnown));
        if (previous != null) previous.task().cancel();
    }

    private boolean consumeFreshDisconnectHint(UUID playerUuid) {
        Long hintAt = disconnectHints.remove(playerUuid);
        return hintAt != null && (System.currentTimeMillis() - hintAt) <= DISCONNECT_HINT_TTL_MILLIS;
    }

    private void finalizeOffline(UUID playerUuid, String playerName, RemotePlayerEntry lastKnown) {
        Boolean stillOurs = store.claimOfflineIfOwned(playerUuid, playerName, identity.instanceId());
        if (Boolean.FALSE.equals(stillOurs)) return;

        if (platform.getSessionViolationStore() != null) {
            platform.getSessionViolationStore().onPlayerQuit(playerUuid);
        }
        notifyPlayerOffline(playerUuid, lastKnown);
        if (!platform.getRedisRepository().isConnected()) return;
        Packet<SyncPlayerOfflinePacket.Payload> packet = Packets.SYNC_PLAYER_OFFLINE.packet();
        platform.getRedisRepository().publish(packet, new SyncPlayerOfflinePacket.Payload(
                identity.instanceId(), playerUuid, playerName));
    }

    private void cancelPendingOffline(UUID playerUuid) {
        PendingOffline pending = pendingOffline.remove(playerUuid);
        if (pending != null) pending.task().cancel();
    }

    private @Nullable PendingOffline takePendingOffline(UUID playerUuid) {
        PendingOffline pending = pendingOffline.remove(playerUuid);
        if (pending != null) pending.task().cancel();
        return pending;
    }

    public void onProxyPlayerJoin(@NotNull UUID playerUuid) {
        cancelPendingOffline(playerUuid);
        disconnectHints.remove(playerUuid);
    }

    public void onProxyPlayerSwitch(@NotNull UUID playerUuid, @Nullable UUID destinationInstance) {
        extendPendingForTransfer(playerUuid);
        if (destinationInstance != null) notifyPlayerServerSwitch(playerUuid, destinationInstance);
    }

    public void onProxyPlayerDisconnect(@NotNull UUID playerUuid) {
        PendingOffline pending = takePendingOffline(playerUuid);
        if (pending != null) {
            platform.getScheduler().runAsyncTask(() ->
                    finalizeOffline(playerUuid, pending.playerName(), pending.lastKnown()));
            return;
        }
        TGPlayer cached = platform.getPlayerRepository().getPlayer(playerUuid);
        if (cached != null && cached.getName() != null) {
            String name = cached.getName();
            RemotePlayerEntry lastKnown = new RemotePlayerEntry(
                    playerUuid, name, identity.instanceId(), effectiveDisplayName);
            disconnectHints.put(playerUuid, System.currentTimeMillis());
            platform.getScheduler().runAsyncTask(() -> finalizeOffline(playerUuid, name, lastKnown));
            return;
        }
        disconnectHints.put(playerUuid, System.currentTimeMillis());
    }

    public void onProxyPlayerTransfer(@NotNull UUID playerUuid) {
        disconnectHints.remove(playerUuid);
        extendPendingForTransfer(playerUuid);
    }

    private void extendPendingForTransfer(UUID playerUuid) {
        PendingOffline existing = pendingOffline.remove(playerUuid);
        if (existing == null) return;
        existing.task().cancel();
        schedulePendingOffline(playerUuid, existing.playerName(), existing.lastKnown(),
                TRANSFER_GRACE_MILLIS);
    }

    private long offlineGraceMillis() {
        try {
            int millis = platform.getConfigRepository().config(ConfigFile.CONFIG)
                    .getInt(ConfigKeys.REDIS_OFFLINE_GRACE_MILLIS);
            return Math.max(0, millis);
        } catch (Exception ex) {
            return 4000L;
        }
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
        notifyServerOffline(payload.instanceId());
    }

    public void acceptPlayerJoin(@NotNull SyncPlayerJoinPacket.Payload payload) {
        if (isLocal(payload.instanceId())) return;
        cancelPendingOffline(payload.playerUuid());
        notifyPlayerOnline(payload.playerUuid(), new RemotePlayerEntry(
                payload.playerUuid(), payload.playerName(), payload.instanceId(), payload.serverName()));
    }

    public void acceptPlayerOffline(@NotNull SyncPlayerOfflinePacket.Payload payload) {
        if (isLocal(payload.instanceId())) return;
        if (platform.getPlayerRepository().getPlayer(payload.playerUuid()) != null) return;
        platform.getScheduler().runAsyncTask(() -> {
            if (store.findByUuid(payload.playerUuid()) != null) return;
            notifyPlayerOffline(payload.playerUuid(), new RemotePlayerEntry(
                    payload.playerUuid(), payload.playerName(), payload.instanceId(), ""));
        });
    }

    public void acceptTeleportRequest(@NotNull SyncTeleportRequestPacket.Payload payload) {
        platform.handleIncomingTeleportRequest(payload);
    }

    @Override
    public void onConnected(RedisConnection connection) {
        platform.getScheduler().runAsyncTask(() -> {
            UUID instanceId = identity.instanceId();
            String displayName = effectiveDisplayName;
            Packet<SyncPlayerJoinPacket.Payload> joinPacket = Packets.SYNC_PLAYER_JOIN.packet();

            for (TGPlayer player : platform.getPlayerRepository().getPlayers()) {
                String playerName = player.getName();
                if (playerName == null) continue;
                UUID playerUuid = player.getUuid();
                store.addPlayer(playerUuid, playerName, instanceId, displayName,
                        player.getUser().getProfile());
                platform.getRedisRepository().publish(joinPacket, new SyncPlayerJoinPacket.Payload(
                        instanceId, displayName, playerUuid, playerName));
            }
            publishHeartbeat();
        });
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
            if (platform.getSessionViolationStore() != null) {
                platform.getSessionViolationStore().heartbeat(ownedPlayers);
            }
            if (platform.getModSessionStore() != null) {
                platform.getModSessionStore().heartbeat(ownedPlayers);
            }
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
        Map<UUID, List<RemotePlayerEntry>> purged = store.sweepStaleServers(identity.instanceId());
        store.reconcileOrphanPlayers();
        if (platform.getSessionViolationStore() != null) {
            platform.getSessionViolationStore().reconcileOrphanViolators();
        }
        if (purged.isEmpty()) return;

        Packet<SyncPlayerOfflinePacket.Payload> playerOffline = Packets.SYNC_PLAYER_OFFLINE.packet();
        Packet<SyncServerOfflinePacket.Payload> serverOffline = Packets.SYNC_SERVER_OFFLINE.packet();
        boolean redisEnabled = platform.getRedisRepository().isEnabled();

        for (Map.Entry<UUID, List<RemotePlayerEntry>> entry : purged.entrySet()) {
            UUID iid = entry.getKey();
            List<UUID> sessionUuids = new ArrayList<>(entry.getValue().size());
            for (RemotePlayerEntry player : entry.getValue()) {
                sessionUuids.add(player.playerUuid());
                notifyPlayerOffline(player.playerUuid(), player);
                if (!redisEnabled) continue;
                platform.getRedisRepository().publish(playerOffline, new SyncPlayerOfflinePacket.Payload(
                        iid, player.playerUuid(), player.playerName()));
            }
            if (platform.getSessionViolationStore() != null && !sessionUuids.isEmpty()) {
                platform.getSessionViolationStore().purgeIfOwnedBy(iid, sessionUuids);
            }
            if (platform.getModSessionStore() != null && !sessionUuids.isEmpty()) {
                platform.getModSessionStore().purgeIfOwnedBy(iid, sessionUuids);
            }
            notifyServerOffline(iid);
            if (!redisEnabled) continue;
            platform.getRedisRepository().publish(serverOffline, new SyncServerOfflinePacket.Payload(iid, ""));
        }
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

    private void notifyPlayerOffline(UUID playerUuid, RemotePlayerEntry lastKnown) {
        for (PresenceListener listener : listeners) {
            try {
                listener.onPlayerOffline(playerUuid, lastKnown);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Presence listener threw on offline", ex);
            }
        }
    }

    private void notifyLocalPlayerJoin(UUID playerUuid) {
        for (PresenceListener listener : listeners) {
            try {
                listener.onLocalPlayerJoin(playerUuid);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Presence listener threw on local join", ex);
            }
        }
    }

    private void notifyLocalPlayerQuit(UUID playerUuid) {
        for (PresenceListener listener : listeners) {
            try {
                listener.onLocalPlayerQuit(playerUuid);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Presence listener threw on local quit", ex);
            }
        }
    }

    private void notifyPlayerServerSwitch(UUID playerUuid, UUID destinationInstance) {
        for (PresenceListener listener : listeners) {
            try {
                listener.onPlayerServerSwitch(playerUuid, destinationInstance);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Presence listener threw on server switch", ex);
            }
        }
    }

    private void notifyServerOffline(UUID instanceId) {
        for (PresenceListener listener : listeners) {
            try {
                listener.onServerOffline(instanceId);
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Presence listener threw on server offline", ex);
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

    private record PendingOffline(ScheduledTask task, String playerName, RemotePlayerEntry lastKnown) {
    }
}
