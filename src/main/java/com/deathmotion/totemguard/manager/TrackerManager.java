package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.checks.TotemEventListener;
import com.deathmotion.totemguard.checks.impl.totem.processor.TotemProcessor;
import com.deathmotion.totemguard.models.TotemPlayer;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerManager implements TotemEventListener {

    private final TotemGuard plugin;

    // Map of target players to their tracking information
    private final ConcurrentHashMap<Player, TargetTracker> targetTrackers = new ConcurrentHashMap<>();
    // Map of viewer players to the TargetTracker they are viewing
    private final ConcurrentHashMap<Player, TargetTracker> viewerToTracker = new ConcurrentHashMap<>();

    public TrackerManager(TotemGuard plugin) {
        this.plugin = plugin;

        TotemProcessor.getInstance().registerListener(this);
        startScheduler();
    }

    private void startScheduler() {
        FoliaScheduler.getAsyncScheduler().runAtFixedRate(plugin, task -> {
            for (TargetTracker tracker : targetTrackers.values()) {
                sendActionBarToViewers(tracker);
            }
        }, 0L, 20L); // Run every second (20 ticks)
    }

    @Override
    public void onTotemEvent(Player player, TotemPlayer totemPlayer) {
        long latestInterval = totemPlayer.totemData().getLatestIntervals(1).get(0);

        TargetTracker tracker = targetTrackers.get(player);
        if (tracker != null) {
            tracker.setLatestData(new TotemData(latestInterval, Instant.now()));
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

        viewer.sendMessage(Component.text("You are now tracking " + target.getName() + ".", NamedTextColor.GREEN));

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

            viewer.sendMessage(Component.text("You are no longer tracking " + tracker.getTarget().getName() + ".", NamedTextColor.YELLOW));
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
            Component message = Component.text(playerName + " has disconnected.", NamedTextColor.RED);

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
                viewer.sendMessage(Component.text("Stopped tracking offline player.", NamedTextColor.YELLOW));
                viewer.sendActionBar(Component.empty());
            }
        }
    }

    private Component createTrackingMessage(Player target, TotemData data) {
        String timeAgo = formatTimeAgo(data.lastUpdated());

        return Component.text()
                .append(Component.text("Tracking: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(target.getName(), NamedTextColor.GOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Totem Speed: ", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text(data.latestInterval() + "ms", NamedTextColor.GOLD))
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
