/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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
import com.deathmotion.totemguard.api.events.AlertsToggleEvent;
import com.deathmotion.totemguard.api.interfaces.AlertManager;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.database.DatabaseProvider;
import com.deathmotion.totemguard.messenger.MessengerService;
import com.deathmotion.totemguard.redis.packet.impl.SyncAlertMessagePacket;
import com.deathmotion.totemguard.util.datastructure.Pair;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManagerImpl implements AlertManager {

    @Getter
    private final ConcurrentHashMap<UUID, Player> enabledAlerts;

    private final TotemGuard plugin;
    private final DatabaseProvider databaseProvider;
    private final MessengerService messageService;

    public AlertManagerImpl(TotemGuard pluginInstance) {
        this.plugin = pluginInstance;
        this.databaseProvider = pluginInstance.getDatabaseProvider();
        this.messageService = pluginInstance.getMessengerService();
        this.enabledAlerts = new ConcurrentHashMap<>();
    }

    /**
     * Sends an alert message derived from a given Check and details to all players
     * who have alerts enabled, as well as to console and other integrations if configured.
     *
     * @param check   The Check instance representing the type of alert.
     * @param details Additional message details.
     */
    public void sendAlert(Check check, Component details) {
        Pair<Component, Component> craftedAlert = messageService.createAlert(check, details);

        // Send to all players who have alerts enabled
        enabledAlerts.values().forEach(player -> player.sendMessage(craftedAlert.getX()));

        // Optionally log to console
        if (plugin.getConfigManager().getSettings().isConsoleAlerts()) {
            plugin.getServer().getConsoleSender().sendMessage(craftedAlert.getY());
        }

        // Send to proxy and Discord if enabled
        plugin.getRedisService().publish(new SyncAlertMessagePacket(), new SyncAlertMessagePacket.AlertComponents(craftedAlert.getX(), craftedAlert.getY()));
        plugin.getDiscordManager().sendAlert(check, details);
        databaseProvider.getAlertRepository().storeAlert(check);
    }

    /**
     * Sends a generic alert message to everyone with alerts enabled.
     *
     * @param message The text component to be broadcast.
     */
    public void sendAlert(SyncAlertMessagePacket.AlertComponents message) {
        // Optionally log to console
        if (plugin.getConfigManager().getSettings().isConsoleAlerts()) {
            plugin.getServer().getConsoleSender().sendMessage(message.consoleAlert);
        }

        enabledAlerts.values().forEach(player -> player.sendMessage(message.gameAlert));
    }

    /**
     * Toggles the alert status for a given player. If alerts are currently enabled for them,
     * they will be disabled; if disabled, they will be enabled. An AlertsToggleEvent is fired
     * if API usage is enabled, and if the event is not canceled.
     *
     * @param player The player whose alerts will be toggled.
     * @return True if the toggle was successful; false if the event was canceled or other issues arose.
     */
    public boolean toggleAlerts(Player player) {
        UUID playerId = player.getUniqueId();

        boolean currentlyEnabled = enabledAlerts.containsKey(playerId);
        boolean willEnable = !currentlyEnabled;

        // Check whether this toggling action is allowed by any external event listeners
        if (!canToggleAlerts(player, willEnable)) {
            return false;
        }

        // Perform the toggle
        if (currentlyEnabled) {
            enabledAlerts.remove(playerId);
            player.sendMessage(messageService.toggleAlerts(false));
        } else {
            enabledAlerts.put(playerId, player);
            player.sendMessage(messageService.toggleAlerts(true));
        }

        return true;
    }

    /**
     * Enables alerts for a specified player if not already enabled.
     *
     * @param player The player for whom alerts will be enabled.
     * @return True if alerts were successfully enabled; false if the event was canceled or other issues arose.
     */
    public boolean enableAlerts(Player player) {
        UUID playerId = player.getUniqueId();

        // If alerts are already enabled, do nothing
        if (enabledAlerts.containsKey(playerId)) {
            return true;
        }

        // Check if we can enable them (event not canceled, etc.)
        if (!canToggleAlerts(player, true)) {
            return false;
        }

        enabledAlerts.put(playerId, player);
        player.sendMessage(messageService.toggleAlerts(true));
        return true;
    }

    /**
     * Removes a player's UUID from the alert list, effectively disabling alerts for them.
     *
     * @param playerId The player's UUID to remove from active alerts.
     */
    public void removePlayer(UUID playerId) {
        enabledAlerts.remove(playerId);
    }

    /**
     * Checks if a given player has alerts enabled.
     *
     * @param player The player to check.
     * @return True if the player has alerts enabled; otherwise false.
     */
    public boolean hasAlertsEnabled(Player player) {
        return enabledAlerts.containsKey(player.getUniqueId());
    }

    /**
     * Handles player disconnects (removes them from the active alerts).
     *
     * @param player The player who is quitting.
     */
    public void handlePlayerQuit(Player player) {
        enabledAlerts.remove(player.getUniqueId());
    }

    /**
     * Helper method to centralize event firing logic.
     * If the API is disabled or
     * there is no valid TotemPlayer, this returns true without firing an event.
     * Otherwise, it fires an AlertsToggleEvent and returns false if the event was canceled.
     *
     * @param player Player instance
     * @param enable Whether we intend to enable or disable alerts.
     * @return True if toggling can proceed (event not canceled); false otherwise.
     */
    private boolean canToggleAlerts(Player player, boolean enable) {
        if (!TotemGuard.getInstance().getConfigManager().getSettings().isApi()) {
            return true;
        }

        // Create and fire the event
        AlertsToggleEvent event = new AlertsToggleEvent(player, enable);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}