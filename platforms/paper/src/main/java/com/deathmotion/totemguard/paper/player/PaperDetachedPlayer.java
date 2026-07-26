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

package com.deathmotion.totemguard.paper.player;

import com.deathmotion.totemguard.common.platform.player.DetachedPlayer;
import com.deathmotion.totemguard.common.platform.player.WorldAnchor;
import com.deathmotion.totemguard.paper.scheduler.PaperScheduler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class PaperDetachedPlayer implements DetachedPlayer {

    private static final int PARKED_VIEW_DISTANCE = 2;
    private static final int PARKED_CHUNK_SPAN = PARKED_VIEW_DISTANCE * 2 + 1;
    private static final long REATTACH_SETTLE_MILLIS = 400L;

    private final Player player;
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private final AtomicBoolean released = new AtomicBoolean();
    private final QuitGuard quitGuard = new QuitGuard();
    private final Location home;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final boolean invulnerable;
    private final int viewDistance;
    private volatile @Nullable Runnable whenBack;

    private PaperDetachedPlayer(Player player, Plugin plugin, PaperScheduler scheduler) {
        this.player = player;
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.home = player.getLocation().clone();
        this.gameMode = player.getGameMode();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.invulnerable = player.isInvulnerable();
        this.viewDistance = readViewDistance(player);
    }

    static DetachedPlayer detach(Player player, Plugin plugin, PaperScheduler scheduler) {
        PaperDetachedPlayer detached = new PaperDetachedPlayer(player, plugin, scheduler);
        Bukkit.getPluginManager().registerEvents(detached.quitGuard, plugin);
        scheduler.runForEntity(player, detached::park, null);
        return detached;
    }

    private static int readViewDistance(Player player) {
        try {
            return player.getViewDistance();
        } catch (NoSuchMethodError | UnsupportedOperationException unsupported) {
            return -1;
        }
    }

    private void park() {
        if (!player.isOnline()) return;
        player.closeInventory();
        player.setGameMode(GameMode.SPECTATOR);
        player.setInvulnerable(true);
        for (Player other : Bukkit.getOnlinePlayers()) {
            conceal(other);
        }
    }

    @Override
    public @Nullable WorldAnchor anchor() {
        World world = home.getWorld();
        if (world == null) return null;
        int distance = viewDistance > 0 ? viewDistance : world.getViewDistance();
        return new WorldAnchor(world.getKey().toString(),
                home.getBlockX() >> 4, home.getBlockZ() >> 4, distance, gameMode.name());
    }

    @Override
    public void hideFrom(@NotNull UUID joined) {
        if (released.get()) return;
        scheduler.runForEntity(player, () -> {
            Player other = Bukkit.getPlayer(joined);
            if (other != null) conceal(other);
        }, null);
    }

    @Override
    public void reattach(@NotNull Runnable whenBack) {
        if (!released.compareAndSet(false, true)) return;
        HandlerList.unregisterAll(quitGuard);
        this.whenBack = whenBack;
        scheduler.runForEntity(player, this::restore, null);
    }

    private void signalBack() {
        Runnable done = this.whenBack;
        this.whenBack = null;
        if (done != null) done.run();
    }

    private void conceal(Player other) {
        if (other == player || !other.isOnline() || !player.isOnline()) return;
        player.hidePlayer(plugin, other);
        other.hidePlayer(plugin, player);
    }

    private void reveal(Player other) {
        if (other == player || !other.isOnline() || !player.isOnline()) return;
        player.showPlayer(plugin, other);
        other.showPlayer(plugin, player);
    }

    private void restore() {
        if (!player.isOnline()) {
            signalBack();
            return;
        }
        for (Player other : Bukkit.getOnlinePlayers()) {
            reveal(other);
        }
        resyncWorld(player);
    }

    private void restoreState() {
        if (!player.isOnline()) return;
        player.setGameMode(gameMode);
        player.setInvulnerable(invulnerable);
        player.setAllowFlight(allowFlight);
        if (allowFlight) player.setFlying(flying);
        player.updateInventory();
        try {
            player.sendHealthUpdate();
        } catch (NoSuchMethodError | UnsupportedOperationException ignored) {
        }
        player.setLevel(player.getLevel());
        player.setExp(player.getExp());
        Location compass = player.getCompassTarget();
        if (compass != null) player.setCompassTarget(compass);
    }

    private void resyncWorld(Player player) {
        applyViewDistance(PARKED_VIEW_DISTANCE);
        Location hop = hopTarget();

        player.teleportAsync(hop).thenRun(() -> scheduler.runAsyncTaskDelayed(
                () -> scheduler.runForEntity(player, this::comeHome, null),
                REATTACH_SETTLE_MILLIS, TimeUnit.MILLISECONDS));
    }

    private void comeHome() {
        if (!player.isOnline()) {
            applyViewDistance(viewDistance);
            signalBack();
            return;
        }
        player.teleportAsync(home).thenRun(() -> scheduler.runForEntity(player, () -> {
            applyViewDistance(viewDistance);
            restoreState();
            scheduler.runAsyncTaskDelayed(this::signalBack,
                    REATTACH_SETTLE_MILLIS, TimeUnit.MILLISECONDS);
        }, null));
    }

    private Location hopTarget() {
        int span = Math.max(PARKED_CHUNK_SPAN, viewDistance * 2 + 2) * 16;
        Location spawn = home.getWorld() == null ? null : home.getWorld().getSpawnLocation();
        if (spawn != null && spawn.distanceSquared(home) > (double) span * span) {
            return spawn;
        }
        return home.clone().add(span, 0.0, 0.0);
    }

    private void applyViewDistance(int distance) {
        if (distance < 0 || !player.isOnline()) return;
        try {
            player.setViewDistance(distance);
        } catch (NoSuchMethodError | UnsupportedOperationException | IllegalArgumentException ignored) {
        }
    }

    private final class QuitGuard implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onQuit(PlayerQuitEvent event) {
            if (!event.getPlayer().getUniqueId().equals(player.getUniqueId())) return;
            if (!released.compareAndSet(false, true)) return;
            HandlerList.unregisterAll(this);
            restoreState();
            signalBack();
        }
    }
}
