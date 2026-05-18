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

package com.deathmotion.totemguard.paper.network;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.network.NetworkPresenceRepository;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.deathmotion.totemguard.common.redis.broker.packets.impl.SyncTeleportRequestPacket;
import com.deathmotion.totemguard.paper.TGPaperPlatform;
import com.github.retrooper.packetevents.protocol.world.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class PaperCrossServerTeleportRouter implements Listener {

    private static final long SWEEP_PERIOD_SECONDS = 60L;

    private final TGPlatform platform;
    private final ConcurrentHashMap<UUID, Pending> pending = new ConcurrentHashMap<>();

    public PaperCrossServerTeleportRouter(TGPlatform platform) {
        this.platform = platform;
        Bukkit.getPluginManager().registerEvents(this, ((TGPaperPlatform) platform).getPlugin());
        platform.getScheduler().runAsyncTaskAtFixedRate(
                this::sweep, SWEEP_PERIOD_SECONDS, SWEEP_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Pending p = pending.remove(uuid);
        if (p == null) return;
        if (p.expiresAt < System.currentTimeMillis()) return;
        applyTeleport(uuid, p.targetUuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) return;
        pending.remove(event.getPlayer().getUniqueId());
    }

    public void accept(SyncTeleportRequestPacket.Payload payload) {
        NetworkPresenceRepository presence = platform.getNetworkPresenceRepository();
        if (presence == null) return;
        if (!presence.isLocal(payload.targetServerInstance())) return;
        if (payload.expiresAt() < System.currentTimeMillis()) return;

        if (Bukkit.getPlayer(payload.subjectUuid()) != null) {
            applyTeleport(payload.subjectUuid(), payload.targetUuid());
            return;
        }

        pending.put(payload.subjectUuid(), new Pending(payload.targetUuid(), payload.expiresAt()));
    }

    private void applyTeleport(UUID subjectUuid, UUID targetUuid) {
        PlatformPlayer subjectPlatform = platform.getPlatformPlayerFactory().create(subjectUuid);
        if (subjectPlatform == null) return;

        TGPlayer target = platform.getPlayerRepository().getPlayer(targetUuid);
        if (target != null) {
            PlatformPlayer targetPlatform = target.getPlatformPlayer();
            if (targetPlatform == null) return;
            MovementData movement = target.getData().getMovementData();
            Location loc = movement.getCurrent();
            String world = targetPlatform.getWorldName();
            subjectPlatform.teleport(
                    world == null ? "" : world,
                    loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
            return;
        }

        Player paperTarget = Bukkit.getPlayer(targetUuid);
        if (paperTarget == null || !paperTarget.isOnline()) return;
        org.bukkit.Location loc = paperTarget.getLocation();
        String world = loc.getWorld() == null ? "" : loc.getWorld().getName();
        subjectPlatform.teleport(world,
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Pending> it = pending.values().iterator();
        while (it.hasNext()) {
            if (it.next().expiresAt < now) it.remove();
        }
    }

    private record Pending(UUID targetUuid, long expiresAt) {
    }
}
