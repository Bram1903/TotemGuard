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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerManager {
    // Map of target UUIDs to a set of UUIDs of players tracking them
    private final ConcurrentHashMap<UUID, Set<UUID>> trackedPlayers = new ConcurrentHashMap<>();

    // Map of viewer UUIDs to the target UUID they are tracking
    private final ConcurrentHashMap<UUID, UUID> viewerToTargetMap = new ConcurrentHashMap<>();

    private final TotemGuard plugin;

    public TrackerManager(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public void TrackPlayer(Player target, Player player) {
        UUID targetUUID = target.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        // Ensure the player is only tracking one target
        UntrackPlayer(player);

        // Add the player to the viewers set for the target
        trackedPlayers.computeIfAbsent(targetUUID, k -> ConcurrentHashMap.newKeySet()).add(playerUUID);

        // Update the viewer-to-target map
        viewerToTargetMap.put(playerUUID, targetUUID);

        player.sendMessage(Component.text("You are now tracking " + target.getName() + ".", NamedTextColor.GREEN));
    }

    public void UntrackPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();

        // Check if the player is tracking someone
        UUID targetUUID = viewerToTargetMap.remove(playerUUID);
        if (targetUUID != null) {
            Set<UUID> viewers = trackedPlayers.get(targetUUID);
            if (viewers != null) {
                viewers.remove(playerUUID);
                if (viewers.isEmpty()) {
                    trackedPlayers.remove(targetUUID);
                }
            }

            String targetName = getPlayerName(targetUUID);
            player.sendMessage(Component.text("You are no longer tracking " + targetName + ".", NamedTextColor.YELLOW));
        }
    }

    public boolean IsTrackingPlayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        return viewerToTargetMap.containsKey(playerUUID);
    }

    public void playerDisconnect(UUID playerUUID) {
        // First, handle if the disconnected player is a target being tracked
        Set<UUID> viewers = trackedPlayers.remove(playerUUID);
        if (viewers != null && !viewers.isEmpty()) {
            String playerName = getPlayerName(playerUUID);
            Component message = Component.text(playerName + " has disconnected.", NamedTextColor.RED);

            // Collect online viewers
            for (UUID viewerUUID : viewers) {
                Player viewer = Bukkit.getPlayer(viewerUUID);
                if (viewer != null && viewer.isOnline()) {
                    viewer.sendMessage(message);
                    // Remove tracking information for each viewer
                    viewerToTargetMap.remove(viewerUUID);
                }
            }
        }

        // Then, check if the disconnected player was tracking someone
        UUID targetUUID = viewerToTargetMap.remove(playerUUID);
        if (targetUUID != null) {
            Set<UUID> viewersSet = trackedPlayers.get(targetUUID);
            if (viewersSet != null) {
                viewersSet.remove(playerUUID);
                if (viewersSet.isEmpty()) {
                    trackedPlayers.remove(targetUUID);
                }
            }
        }
    }

    private String getPlayerName(UUID playerUUID) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        } else {
            // If the player is offline, attempt to get their name from the server
            return Bukkit.getOfflinePlayer(playerUUID).getName();
        }
    }
}
