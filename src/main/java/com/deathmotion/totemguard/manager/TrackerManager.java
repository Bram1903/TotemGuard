/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.TotemEventListener;
import com.deathmotion.totemguard.checks.impl.totem.processor.TotemProcessor;
import com.deathmotion.totemguard.models.TotemPlayer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerManager implements TotemEventListener {

    private final TotemGuard plugin;

    // Map of viewer UUIDs to their tracked target UUIDs
    private final ConcurrentHashMap<UUID, UUID> viewerToTargetMap = new ConcurrentHashMap<>();
    // Map of target UUIDs to the set of viewer UUIDs tracking them
    private final ConcurrentHashMap<UUID, Set<UUID>> targetToViewersMap = new ConcurrentHashMap<>();
    // Map of target UUIDs to their latest data
    private final ConcurrentHashMap<UUID, TotemData> latestDataMap = new ConcurrentHashMap<>();

    public TrackerManager(TotemGuard plugin) {
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
        startScheduler();
    }

    private void startScheduler() {
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (UUID targetUUID : targetToViewersMap.keySet()) {
                sendActionBarToViewers(targetUUID);
            }
        }, 0L, 20L); // Run every second (20 ticks)
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        long latestInterval = totemPlayer.totemData().getLatestIntervals(1).get(0);

        // Update the latest data for this target player
        latestDataMap.put(player.getUniqueId(), new TotemData(latestInterval, Instant.now()));

        // Send updated action bar messages to viewers immediately
        sendActionBarToViewers(player.getUniqueId());
    }

    public void startTracking(Player viewer, Player target) {
        UUID viewerUUID = viewer.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        // Ensure the viewer is only tracking one target
        stopTracking(viewerUUID);

        // Add the viewer to the set of viewers for the target
        viewerToTargetMap.put(viewerUUID, targetUUID);
        targetToViewersMap.computeIfAbsent(targetUUID, k -> ConcurrentHashMap.newKeySet()).add(viewerUUID);

        viewer.sendMessage(Component.text("You are now tracking " + target.getName() + ".", NamedTextColor.GREEN));

        // Send the action bar message immediately
        sendActionBarToViewer(viewer, targetUUID);
    }

    public void stopTracking(Player viewer) {
        stopTracking(viewer.getUniqueId());
    }

    private void stopTracking(UUID viewerUUID) {
        UUID targetUUID = viewerToTargetMap.remove(viewerUUID);
        if (targetUUID != null) {
            Set<UUID> viewers = targetToViewersMap.get(targetUUID);
            if (viewers != null) {
                viewers.remove(viewerUUID);
                if (viewers.isEmpty()) {
                    targetToViewersMap.remove(targetUUID);
                    latestDataMap.remove(targetUUID); // Optionally remove data if no one is tracking
                }
            }
            Player viewer = Bukkit.getPlayer(viewerUUID);
            if (viewer != null && viewer.isOnline()) {
                Player target = Bukkit.getPlayer(targetUUID);
                String targetName = (target != null) ? target.getName() : "player";
                viewer.sendMessage(Component.text("You are no longer tracking " + targetName + ".", NamedTextColor.YELLOW));

                // Clear the action bar message immediately
                viewer.sendActionBar(Component.empty());
            }
        }
    }

    public boolean isTracking(Player viewer) {
        return viewerToTargetMap.containsKey(viewer.getUniqueId());
    }

    public void handlePlayerDisconnect(UUID playerUUID) {
        // Stop tracking if the disconnected player was a viewer
        stopTracking(playerUUID);

        // Remove and notify viewers if the disconnected player was a target
        Set<UUID> viewerUUIDs = targetToViewersMap.remove(playerUUID);
        if (viewerUUIDs != null) {
            String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
            Component message = Component.text(playerName + " has disconnected.", NamedTextColor.RED);

            for (UUID viewerUUID : viewerUUIDs) {
                viewerToTargetMap.remove(viewerUUID);
                Player viewer = Bukkit.getPlayer(viewerUUID);
                if (viewer != null && viewer.isOnline()) {
                    viewer.sendMessage(message);

                    // Clear the action bar message immediately
                    viewer.sendActionBar(Component.empty());
                }
            }
            latestDataMap.remove(playerUUID);
        }
    }

    private void sendActionBarToViewers(UUID targetUUID) {
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null) {
            // Target is offline, remove from tracking
            removeTarget(targetUUID);
            return;
        }

        TotemData data = latestDataMap.get(targetUUID);
        Component message = (data != null)
                ? createTrackingMessage(target, data)
                : Component.text("No data available yet.", NamedTextColor.YELLOW);

        Set<UUID> viewerUUIDs = targetToViewersMap.get(targetUUID);
        if (viewerUUIDs != null) {
            for (UUID viewerUUID : viewerUUIDs) {
                Player viewer = Bukkit.getPlayer(viewerUUID);
                if (viewer != null && viewer.isOnline()) {
                    viewer.sendActionBar(message);
                } else {
                    stopTracking(viewerUUID);
                }
            }
        }
    }

    private void sendActionBarToViewer(Player viewer, UUID targetUUID) {
        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !viewer.isOnline()) {
            return;
        }

        TotemData data = latestDataMap.get(targetUUID);
        Component message = (data != null)
                ? createTrackingMessage(target, data)
                : Component.text("No data available yet.", NamedTextColor.YELLOW);

        viewer.sendActionBar(message);
    }

    private void removeTarget(UUID targetUUID) {
        // Remove target from tracking when they go offline
        Set<UUID> viewerUUIDs = targetToViewersMap.remove(targetUUID);
        if (viewerUUIDs != null) {
            for (UUID viewerUUID : viewerUUIDs) {
                viewerToTargetMap.remove(viewerUUID);
                Player viewer = Bukkit.getPlayer(viewerUUID);
                if (viewer != null && viewer.isOnline()) {
                    viewer.sendMessage(Component.text("Stopped tracking offline player.", NamedTextColor.YELLOW));

                    // Clear the action bar message immediately
                    viewer.sendActionBar(Component.empty());
                }
            }
            latestDataMap.remove(targetUUID);
        }
    }

    private Component createTrackingMessage(Player target, TotemData data) {
        String timeAgo = formatTimeAgo(data.lastUpdated);

        return Component.text()
                .append(Component.text("Tracking: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(target.getName(), NamedTextColor.GOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Totem Speed: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(data.latestInterval + "ms", NamedTextColor.GOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Last Update: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(timeAgo, NamedTextColor.GOLD))
                .build();
    }

    private String formatTimeAgo(Instant lastUpdated) {
        Duration duration = Duration.between(lastUpdated, Instant.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return seconds + "s ago";
        } else {
            long minutes = seconds / 60;
            return minutes + "m ago";
        }
    }

    private record TotemData(long latestInterval, Instant lastUpdated) {
    }
}