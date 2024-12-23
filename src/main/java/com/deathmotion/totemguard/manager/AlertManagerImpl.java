/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2024 Bram and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.deathmotion.totemguard.manager;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.api.interfaces.AlertManager;
import com.deathmotion.totemguard.checks.Check;
import com.deathmotion.totemguard.messenger.MessengerService;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManagerImpl implements AlertManager {

    @Getter
    private final ConcurrentHashMap<UUID, Player> enabledAlerts;

    private final TotemGuard plugin;
    private final MessengerService messengerService;

    public AlertManagerImpl(TotemGuard plugin) {
        this.plugin = plugin;
        this.messengerService = plugin.getMessengerService();

        this.enabledAlerts = new ConcurrentHashMap<>();
    }

    public void sendAlert(Check check, Component details) {
        Component alert = messengerService.createAlert(check, details);
        enabledAlerts.values().forEach(player -> player.sendMessage(alert));
    }

    public void sendAlert(Component message) {
        enabledAlerts.values().forEach(player -> player.sendMessage(message));
    }

    public void toggleAlerts(Player player) {
        UUID playerId = player.getUniqueId();
        if (enabledAlerts.containsKey(playerId)) {
            enabledAlerts.remove(playerId);
            player.sendMessage(messengerService.toggleAlerts(false));
        } else {
            enabledAlerts.put(playerId, player);
            player.sendMessage(messengerService.toggleAlerts(true));
        }
    }

    public void enableAlerts(Player player) {
        UUID playerId = player.getUniqueId();
        if (!enabledAlerts.containsKey(playerId)) {
            enabledAlerts.put(playerId, player);
            player.sendMessage(messengerService.toggleAlerts(true));
        }
    }

    public void removePlayer(UUID playerId) {
        enabledAlerts.remove(playerId);
    }

    public boolean hasAlertsEnabled(Player player) {
        return enabledAlerts.containsKey(player.getUniqueId());
    }
}
