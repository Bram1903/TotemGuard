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

package com.deathmotion.totemguard.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.data.TotemPlayer;
import com.deathmotion.totemguard.manager.AlertManager;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserTracker implements PacketListener {
    private final ConcurrentHashMap<UUID, TotemPlayer> totemPlayers = new ConcurrentHashMap<>();

    private final AlertManager alertManager;

    public UserTracker(TotemGuard plugin) {
        this.alertManager = plugin.getAlertManager();
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        User user = event.getUser();

        UUID userUUID = user.getUUID();
        if (userUUID == null) return;

        Player player = (Player) event.getPlayer();

        if (player.hasPermission("TotemGuard.Alerts")) {
            alertManager.enableAlerts(player);
        }

        TotemPlayer totemPlayer = new TotemPlayer();
        totemPlayer.setUuid(userUUID);
        totemPlayer.setUsername(player.getName());
        totemPlayer.setClientBrandName(Objects.requireNonNullElse(player.getClientBrandName(), "Unknown"));
        totemPlayer.setClientVersion(user.getClientVersion());
        totemPlayer.setBedrockPlayer(userUUID.getMostSignificantBits() == 0L);

        totemPlayers.put(userUUID, totemPlayer);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        UUID userUUID = event.getUser().getUUID();
        if (userUUID == null) return;

        alertManager.removePlayer(userUUID);
        totemPlayers.remove(userUUID);
    }

    public Optional<TotemPlayer> getTotemPlayer(UUID uuid) {
        return Optional.ofNullable(totemPlayers.get(uuid));
    }
}
