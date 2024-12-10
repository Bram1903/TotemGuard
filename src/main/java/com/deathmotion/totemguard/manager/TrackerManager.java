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
import com.deathmotion.totemguard.api.events.TotemCycleEvent;
import com.deathmotion.totemguard.util.MathUtil;
import com.deathmotion.totemguard.util.MessageService;
import com.deathmotion.totemguard.util.datastructure.Pair;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerManager implements Listener {

    private final TotemGuard plugin;
    private final MessageService messageService;

    // Map of target players to their tracking information
    private final ConcurrentHashMap<Player, TargetTracker> targetTrackers = new ConcurrentHashMap<>();
    // Map of viewer players to the TargetTracker they are viewing
    private final ConcurrentHashMap<Player, TargetTracker> viewerToTracker = new ConcurrentHashMap<>();

    public TrackerManager(TotemGuard plugin) {
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        startScheduler();
    }

    private void startScheduler() {
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (TargetTracker tracker : targetTrackers.values()) {
                sendActionBarToViewers(tracker);
            }
        }, 0L, 20L); // Run every second (20 ticks)
    }

    private Component getPrefix() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfigManager().getSettings().getPrefix());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemCycle(TotemCycleEvent event) {
        List<Long> intervals = event.getTotemPlayer().totemData().getLatestIntervals(5);
        if (intervals.isEmpty()) return;

        long latestInterval = intervals.get(0);
        double averageInterval = MathUtil.trim(2, MathUtil.getMean(intervals));

        double stDev = 0;
        if (intervals.size() > 1) {
            stDev = MathUtil.trim(2, MathUtil.getStandardDeviation(intervals));
        }

        TargetTracker tracker = targetTrackers.get(event.getPlayer());
        if (tracker != null) {
            tracker.setLatestData(new TotemData(latestInterval, averageInterval, stDev, Instant.now()));
            sendActionBarToViewers(tracker);
        }
    }

    public void startTracking(Player viewer, Player target) {
        // Stop tracking current target if any
        stopTracking(viewer);

        // Get or create TargetTracker for target
        TargetTracker tracker = targetTrackers.computeIfAbsent(target, TargetTracker::new);

        // Add viewer to tracker's viewers
        tracker.addViewer(viewer);

        // Map viewer to tracker
        viewerToTracker.put(viewer, tracker);

        viewer.sendMessage(getPrefix().append(Component.text("You are now tracking " + target.getName() + ".", NamedTextColor.GREEN)));

        // Send the action bar message immediately
        sendActionBarToViewer(viewer, tracker);
    }

    public void stopTracking(Player viewer) {
        TargetTracker tracker = viewerToTracker.remove(viewer);
        if (tracker != null) {
            tracker.removeViewer(viewer);

            if (tracker.getViewers().isEmpty()) {
                targetTrackers.remove(tracker.getTarget());
            }

            viewer.sendMessage(getPrefix().append(Component.text("You are no longer tracking " + tracker.getTarget().getName() + ".", NamedTextColor.YELLOW)));
            viewer.sendActionBar(Component.empty());
        }
    }

    public boolean isTracking(Player viewer) {
        return viewerToTracker.containsKey(viewer);
    }

    public void handlePlayerDisconnect(UUID playerUUID) {
        // Remove viewer from viewerToTracker
        Player viewerToRemove = null;
        for (Player viewer : viewerToTracker.keySet()) {
            if (viewer.getUniqueId().equals(playerUUID)) {
                viewerToRemove = viewer;
                break;
            }
        }
        if (viewerToRemove != null) {
            stopTracking(viewerToRemove);
        }

        // Remove target from targetTrackers
        Player targetToRemove = null;
        for (Player target : targetTrackers.keySet()) {
            if (target.getUniqueId().equals(playerUUID)) {
                targetToRemove = target;
                break;
            }
        }
        if (targetToRemove != null) {
            TargetTracker tracker = targetTrackers.remove(targetToRemove);
            String playerName = targetToRemove.getName();
            Component message = getPrefix().append(Component.text(playerName + " has disconnected.", NamedTextColor.RED));

            for (Player viewer : tracker.getViewers()) {
                viewerToTracker.remove(viewer);
                if (viewer.isOnline()) {
                    viewer.sendMessage(message);
                    viewer.sendActionBar(Component.empty());
                }
            }
        }
    }

    private void sendActionBarToViewers(TargetTracker tracker) {
        TotemData data = tracker.getLatestData();
        Player target = tracker.getTarget();

        if (target == null || !target.isOnline()) {
            removeTargetTracker(tracker);
            return;
        }

        Component message = (data != null)
                ? createTrackingMessage(target, data)
                : Component.text("No data available yet.", NamedTextColor.YELLOW);

        for (Player viewer : tracker.getViewers()) {
            if (viewer != null && viewer.isOnline()) {
                viewer.sendActionBar(message);
            } else {
                stopTracking(viewer);
            }
        }
    }

    private void sendActionBarToViewer(Player viewer, TargetTracker tracker) {
        Player target = tracker.getTarget();
        if (target == null || !viewer.isOnline()) {
            return;
        }

        TotemData data = tracker.getLatestData();
        Component message = (data != null)
                ? createTrackingMessage(target, data)
                : Component.text("No data available yet.", NamedTextColor.YELLOW);

        viewer.sendActionBar(message);
    }

    private void removeTargetTracker(TargetTracker tracker) {
        targetTrackers.remove(tracker.getTarget());
        for (Player viewer : tracker.getViewers()) {
            viewerToTracker.remove(viewer);
            if (viewer.isOnline()) {
                viewer.sendMessage(getPrefix().append(Component.text("Stopped tracking offline player.", NamedTextColor.YELLOW)));
                viewer.sendActionBar(Component.empty());
            }
        }
    }

    private Component createTrackingMessage(Player target, TotemData data) {
        String timeAgo = formatTimeAgo(data.lastUpdated());
        Pair<TextColor, TextColor> colorScheme = messageService.getColorScheme();

        return Component.text()
                .append(Component.text("Tracking: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(target.getName(), colorScheme.getX()))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Speed: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(data.latestInterval() + "ms", colorScheme.getX()))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Average: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(data.averageInterval + "ms", colorScheme.getX()))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Std Dev: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(data.stDev, colorScheme.getX()))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Updated: ", colorScheme.getY(), TextDecoration.BOLD))
                .append(Component.text(timeAgo, colorScheme.getX()))
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

    private record TotemData(long latestInterval, double averageInterval, double stDev, Instant lastUpdated) {
    }

    @Getter
    @Setter
    private static class TargetTracker {
        private final Player target;
        private final Set<Player> viewers = ConcurrentHashMap.newKeySet();
        private TotemData latestData;

        public TargetTracker(Player target) {
            this.target = target;
        }

        public void addViewer(Player viewer) {
            viewers.add(viewer);
        }

        public void removeViewer(Player viewer) {
            viewers.remove(viewer);
        }

    }
}