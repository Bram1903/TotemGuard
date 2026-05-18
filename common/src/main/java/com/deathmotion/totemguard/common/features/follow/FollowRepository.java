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

package com.deathmotion.totemguard.common.features.follow;

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.network.PresenceListener;
import com.deathmotion.totemguard.common.network.ProxyTopologyService;
import com.deathmotion.totemguard.common.network.RemotePlayerEntry;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.redis.broker.packets.Packet;
import com.deathmotion.totemguard.common.redis.broker.packets.Packets;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncFollowEventPacket;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.common.util.ActionBars;
import com.deathmotion.totemguard.common.util.ScheduledTask;
import com.github.retrooper.packetevents.protocol.world.Location;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FollowRepository implements PresenceListener {

    private static final long TICK_PERIOD_MILLIS = 500L;
    private static final long REQUEST_TTL_MILLIS = 30_000L;
    private static final long STALE_SWEEP_PERIOD_SECONDS = 120L;
    // Per-tick distance² above which we treat the target's move as a hard
    // discontinuity (server teleport, admin /tp, end-portal) and therefore also
    // worth broadcasting to remote followers. Tuned to sit above any normal
    // locomotion at a 500 ms tick.
    private static final double TELEPORT_JUMP_THRESHOLD_SQ = 80.0 * 80.0;
    // Separation² between a local follower and the target above which we
    // pull the follower back. Approximate — the user's intent is "around 100".
    private static final double LOCAL_REPULL_THRESHOLD_SQ = 100.0 * 100.0;

    private final TGPlatform platform;
    private final Logger logger;
    private final FollowStore store;
    private final FollowerPositionTracker followerPositions = new FollowerPositionTracker();

    private final ConcurrentHashMap<UUID, FollowState> myFollowers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TargetSnapshot> lastTickSnapshot = new ConcurrentHashMap<>();
    private final AtomicBoolean started = new AtomicBoolean();
    private @Nullable ScheduledTask tickTask;
    private @Nullable ScheduledTask staleSweepTask;

    public FollowRepository(@NotNull TGPlatform platform) {
        this.platform = platform;
        this.logger = platform.getLogger();
        this.store = new FollowStore(platform.getRedisRepository(), this.logger);
    }

    private static double squaredDistance(@NotNull TargetSnapshot a, @NotNull TargetSnapshot b) {
        double dx = a.x() - b.x();
        double dy = a.y() - b.y();
        double dz = a.z() - b.z();
        return dx * dx + dy * dy + dz * dz;
    }

    public void start() {
        if (!started.compareAndSet(false, true)) return;
        tickTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::tick, TICK_PERIOD_MILLIS, TICK_PERIOD_MILLIS, TimeUnit.MILLISECONDS);
        staleSweepTask = platform.getScheduler().runAsyncTaskAtFixedRate(
                this::sweepStaleFollows,
                STALE_SWEEP_PERIOD_SECONDS, STALE_SWEEP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (!started.compareAndSet(true, false)) return;
        if (tickTask != null) tickTask.cancel();
        tickTask = null;
        if (staleSweepTask != null) staleSweepTask.cancel();
        staleSweepTask = null;
        myFollowers.clear();
        lastTickSnapshot.clear();
        followerPositions.clear();
    }

    public void updateFollowerPosition(@NotNull UUID followerUuid, double x, double y, double z) {
        followerPositions.update(followerUuid, x, y, z);
    }

    private void sweepStaleFollows() {
        if (!platform.getRedisRepository().isClusterMode()) return;
        if (!platform.getRedisRepository().isConnected()) return;
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;
        try {
            Map<UUID, FollowState> all = store.loadAll();
            for (UUID follower : all.keySet()) {
                if (presence.findByUuid(follower) != null) continue;
                store.remove(follower);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Stale follow sweep failed", ex);
        }
    }

    public @Nullable FollowState get(@NotNull UUID followerUuid) {
        return myFollowers.get(followerUuid);
    }

    public boolean isFollowing(@NotNull UUID followerUuid) {
        return myFollowers.containsKey(followerUuid);
    }

    public void beginFollow(@NotNull FollowState state) {
        myFollowers.put(state.followerUuid(), state);
        store.save(state);
    }

    public boolean endFollow(@NotNull UUID followerUuid) {
        boolean hadLocal = myFollowers.remove(followerUuid) != null;
        followerPositions.remove(followerUuid);
        store.remove(followerUuid);
        return hadLocal;
    }

    public void acceptRemote(@NotNull SyncFollowEventPacket.Payload payload) {
        if (payload.kind() != SyncFollowEventPacket.KIND_MOVE) return;
        for (FollowState state : myFollowers.values()) {
            if (!state.targetUuid().equals(payload.targetUuid())) continue;
            boolean serverSwitch = !state.targetServerInstance().equals(payload.targetServerInstance());
            if (serverSwitch && !fireServerSwitchEvent(state, payload.targetServerInstance(), payload.targetServerName())) {
                continue;
            }
            if (serverSwitch) {
                state.rebindTarget(payload.targetServerInstance(), payload.targetServerName());
                store.save(state);
            }
            applyTeleport(state, payload);
        }
    }

    private boolean fireServerSwitchEvent(@NotNull FollowState state,
                                          @NotNull UUID newTargetInstance,
                                          @NotNull String newTargetServerName) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        boolean crossServer = presence == null || !presence.isLocal(newTargetInstance);
        TGPlayer targetUser = crossServer ? null : platform.getPlayerRepository().getPlayer(state.targetUuid());
        boolean cancelled = platform.getEventBus().getFollow().fire(
                state.followerUuid(), state.targetUuid(), state.targetName(),
                targetUser, newTargetInstance, newTargetServerName,
                crossServer, true);
        if (cancelled) {
            if (myFollowers.remove(state.followerUuid(), state)) {
                store.remove(state.followerUuid());
                sendFollowerMessage(state, MessagesKeys.FOLLOW_DISABLED, Map.of());
            }
            return false;
        }
        return true;
    }

    private void applyTeleport(@NotNull FollowState state, @NotNull SyncFollowEventPacket.Payload payload) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;

        if (presence.isLocal(payload.targetServerInstance())) {
            PlatformPlayer follower = platform.getPlatformPlayerFactory().create(state.followerUuid());
            if (follower == null) return;
            follower.teleport(payload.world(), payload.x(), payload.y(), payload.z(), payload.yaw(), payload.pitch());
            return;
        }

        routeCrossServer(state, payload.targetServerInstance(), payload.targetServerName());
    }

    private void routeCrossServer(@NotNull FollowState state, @NotNull UUID targetServerInstance,
                                  @NotNull String targetServerName) {
        if (!platform.getRedisRepository().isClusterMode()) return;
        if (!platform.getRedisRepository().isConnected()) return;
        if (!platform.getProxyTopologyService().bridgeAvailable()) {
            dropForUnreachableTarget(state);
            return;
        }
        if (platform.checkRoute(targetServerInstance) == ProxyTopologyService.RouteStatus.NOT_ROUTABLE) {
            dropForUnreachableTarget(state);
            return;
        }

        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;
        SyncTeleportRequestPacket.Payload request = new SyncTeleportRequestPacket.Payload(
                UUID.randomUUID(),
                state.followerUuid(),
                state.targetUuid(),
                targetServerName,
                targetServerInstance,
                System.currentTimeMillis() + REQUEST_TTL_MILLIS
        );
        presence.publishTeleportRequest(request);
        platform.getProxyTopologyService().connectToInstance(state.followerUuid(), targetServerInstance);
    }

    private void dropForUnreachableTarget(@NotNull FollowState state) {
        if (myFollowers.remove(state.followerUuid(), state)) {
            store.remove(state.followerUuid());
            sendFollowerMessage(state, MessagesKeys.FOLLOW_DIFFERENT_PROXY,
                    Map.of("tg_player", state.targetName(), "tg_server", state.targetServerName()));
        }
    }

    private void tick() {
        try {
            tickFollowerHud();
            tickLocalTargets();
        } catch (Exception ex) {
            logger.log(Level.WARNING, "FollowRepository tick failed", ex);
        }
    }

    private void tickFollowerHud() {
        if (myFollowers.isEmpty()) return;
        for (FollowState state : myFollowers.values()) {
            TGPlayer follower = platform.getPlayerRepository().getPlayer(state.followerUuid());
            if (follower == null) continue;
            Component bar = platform.getMessageService().getComponent(
                    MessagesKeys.FOLLOW_ACTION_BAR,
                    Map.<String, Object>of("tg_player", state.targetName(),
                            "tg_server", state.targetServerName()));
            ActionBars.send(follower.getUser(), bar);
        }
    }

    private void tickLocalTargets() {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;
        UUID localInstance = presence.identity().instanceId();
        String localServerName = presence.getLocalServerName();

        for (TGPlayer target : platform.getPlayerRepository().getPlayers()) {
            UUID targetUuid = target.getUuid();
            PlatformPlayer targetPlatform = target.getPlatformPlayer();
            if (targetPlatform == null) continue;
            MovementData movement = target.getData().getMovementData();
            Location loc = movement.getCurrent();
            String world = targetPlatform.getWorldName();
            if (world == null) world = "";

            TargetSnapshot previous = lastTickSnapshot.get(targetUuid);
            TargetSnapshot current = new TargetSnapshot(loc.getX(), loc.getY(), loc.getZ(), world);
            lastTickSnapshot.put(targetUuid, current);

            if (previous == null) continue;
            if (previous.equals(current)) continue;

            boolean worldChanged = !previous.world().equals(world);
            boolean jumped = squaredDistance(previous, current) >= TELEPORT_JUMP_THRESHOLD_SQ;

            applyLocalRepull(targetUuid, world, loc, worldChanged);

            if (worldChanged || jumped) {
                publishMove(targetUuid, localInstance, localServerName, world, loc);
            }
        }
    }

    private void applyLocalRepull(@NotNull UUID targetUuid, @NotNull String world, @NotNull Location loc, boolean worldChanged) {
        for (FollowState state : myFollowers.values()) {
            if (!state.targetUuid().equals(targetUuid)) continue;
            UUID followerUuid = state.followerUuid();
            double[] followerPos = followerPositions.get(followerUuid);
            if (followerPos != null && !worldChanged) {
                double dx = followerPos[0] - loc.getX();
                double dy = followerPos[1] - loc.getY();
                double dz = followerPos[2] - loc.getZ();
                if ((dx * dx + dy * dy + dz * dz) < LOCAL_REPULL_THRESHOLD_SQ) continue;
            }
            teleportLocalFollower(followerUuid, world, loc);
        }
    }

    private void teleportLocalFollower(@NotNull UUID followerUuid, @NotNull String world, @NotNull Location loc) {
        PlatformPlayer follower = platform.getPlatformPlayerFactory().create(followerUuid);
        if (follower == null) return;
        follower.teleport(world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        followerPositions.update(followerUuid, loc.getX(), loc.getY(), loc.getZ());
    }

    private void publishMove(@NotNull UUID targetUuid, @NotNull UUID localInstance,
                             @NotNull String localServerName, @NotNull String world, @NotNull Location loc) {
        if (!platform.getRedisRepository().isClusterMode()) return;
        Packet<SyncFollowEventPacket.Payload> packet = Packets.SYNC_FOLLOW_EVENT.packet();
        platform.getRedisRepository().publish(packet, new SyncFollowEventPacket.Payload(
                SyncFollowEventPacket.KIND_MOVE,
                new UUID(0L, 0L),
                targetUuid,
                new UUID(0L, 0L),
                localInstance,
                localServerName,
                world,
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
        ));
    }

    @Override
    public void onPlayerOnline(UUID playerUuid, RemotePlayerEntry entry) {
        for (FollowState state : myFollowers.values()) {
            if (!state.targetUuid().equals(playerUuid)) continue;
            if (state.targetServerInstance().equals(entry.serverInstanceId())) continue;
            if (entry.bypassed()) {
                dropForBypassedTarget(state, entry.serverName());
                continue;
            }
            state.rebindTarget(entry.serverInstanceId(), entry.serverName());
            store.save(state);
            rerouteAfterServerSwitch(state, entry);
        }
    }

    @Override
    public void onPlayerServerSwitch(UUID playerUuid, UUID destinationInstance) {
        for (FollowState state : myFollowers.values()) {
            if (!state.targetUuid().equals(playerUuid)) continue;
            if (state.targetServerInstance().equals(destinationInstance)) continue;
            NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
            RemotePlayerEntry knownEntry = presence == null ? null : presence.findByUuid(playerUuid);
            if (knownEntry != null && knownEntry.serverInstanceId().equals(destinationInstance) && knownEntry.bypassed()) {
                dropForBypassedTarget(state, knownEntry.serverName());
                continue;
            }
            state.rebindTarget(destinationInstance, state.targetServerName());
            store.save(state);
            if (presence != null && presence.isLocal(destinationInstance)) continue;
            routeCrossServer(state, destinationInstance, state.targetServerName());
        }
    }

    private void dropForBypassedTarget(@NotNull FollowState state, @NotNull String serverName) {
        if (myFollowers.remove(state.followerUuid(), state)) {
            store.remove(state.followerUuid());
            sendFollowerMessage(state, MessagesKeys.FOLLOW_TARGET_BYPASSED,
                    Map.of("tg_player", state.targetName(), "tg_server", serverName));
        }
    }

    private void rerouteAfterServerSwitch(@NotNull FollowState state, @NotNull RemotePlayerEntry entry) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;

        if (presence.isLocal(entry.serverInstanceId())) {
            TGPlayer target = platform.getPlayerRepository().getPlayer(entry.playerUuid());
            if (target == null) return;
            PlatformPlayer targetPlatform = target.getPlatformPlayer();
            PlatformPlayer follower = platform.getPlatformPlayerFactory().create(state.followerUuid());
            if (targetPlatform == null || follower == null) return;
            Location loc = target.getData().getMovementData().getCurrent();
            String world = targetPlatform.getWorldName();
            follower.teleport(world == null ? "" : world,
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            return;
        }

        routeCrossServer(state, entry.serverInstanceId(), entry.serverName());
    }

    @Override
    public void onPlayerOffline(UUID playerUuid, RemotePlayerEntry lastKnown) {
        myFollowers.remove(playerUuid);
        lastTickSnapshot.remove(playerUuid);
        followerPositions.remove(playerUuid);
        store.remove(playerUuid);

        for (FollowState state : myFollowers.values()) {
            if (!state.targetUuid().equals(playerUuid)) continue;
            if (myFollowers.remove(state.followerUuid(), state)) {
                store.remove(state.followerUuid());
                sendFollowerMessage(state, MessagesKeys.FOLLOW_TARGET_OFFLINE,
                        Map.of("tg_player", state.targetName()));
            }
        }
    }

    @Override
    public void onServerOffline(UUID instanceId) {
        for (FollowState state : myFollowers.values()) {
            if (!state.targetServerInstance().equals(instanceId)) continue;
            if (myFollowers.remove(state.followerUuid(), state)) {
                store.remove(state.followerUuid());
                sendFollowerMessage(state, MessagesKeys.FOLLOW_TARGET_OFFLINE,
                        Map.of("tg_player", state.targetName()));
            }
        }
    }

    @Override
    public void onLocalPlayerJoin(UUID playerUuid) {
        if (myFollowers.containsKey(playerUuid)) return;
        platform.getScheduler().runAsyncTask(() -> {
            if (myFollowers.containsKey(playerUuid)) return;
            FollowState state = store.load(playerUuid);
            if (state == null) return;
            myFollowers.putIfAbsent(playerUuid, state);
        });
    }

    @Override
    public void onLocalPlayerQuit(UUID playerUuid) {
        myFollowers.remove(playerUuid);
        lastTickSnapshot.remove(playerUuid);
        followerPositions.remove(playerUuid);
    }

    private void sendFollowerMessage(@NotNull FollowState state,
                                     @NotNull ConfigKey<String> key,
                                     @NotNull Map<String, Object> placeholders) {
        PlatformPlayer follower = platform.getPlatformPlayerFactory().create(state.followerUuid());
        if (follower == null) return;
        follower.sendMessage(platform.getMessageService().getComponent(key, placeholders));
    }

    private record TargetSnapshot(double x, double y, double z, @NotNull String world) {
    }
}
