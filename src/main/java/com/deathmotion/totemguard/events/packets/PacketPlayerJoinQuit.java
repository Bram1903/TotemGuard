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

package com.deathmotion.totemguard.events.packets;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.retrooper.packetevents.util.folia.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class PacketPlayerJoinQuit extends PacketListenerAbstract {

    private final TotemGuard plugin;

    public PacketPlayerJoinQuit(TotemGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS) {
            // Do this after send to avoid sending packets before the PLAY state
            event.getTasksAfterSend().add(() -> TotemGuard.getInstance().getPlayerDataManager().addUser(event.getUser()));
        }
    }

    @Override
    public void onUserLogin(UserLoginEvent event) {
        User user = event.getUser();
        if (user == null) return;

        Player player = event.getPlayer();
        if (player.hasPermission("TotemGuard.Alerts") && player.hasPermission("TotemGuard.Alerts.EnableOnJoin")) {
            FoliaScheduler.getAsyncScheduler().runNow(plugin, (o) -> {
                TotemGuard.getInstance().getAlertManager().toggleAlerts(player);
            });
        }

        if (plugin.getConfigManager().getSettings().getUpdateChecker().isNotifyInGame() && plugin.getUpdateChecker().isUpdateAvailable()) {
            if (player.hasPermission("TotemGuard.Update")) {
                FoliaScheduler.getAsyncScheduler().runDelayed(plugin, (o) -> {
                    player.sendMessage(plugin.getUpdateChecker().getUpdateComponent());
                }, 2, TimeUnit.SECONDS);
            }
        }

        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(user);
        if (totemPlayer == null) return;

        totemPlayer.handlePlayerLogin(player);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        TotemGuard.getInstance().getPlayerDataManager().remove(event.getUser());

        //Check if calling async is safe
        if (event.getUser().getProfile().getUUID() == null) return; // folia doesn't like null getPlayer()
        Player player = Bukkit.getPlayer(event.getUser().getProfile().getUUID());
        if (player != null) {
            TotemGuard.getInstance().getAlertManager().handlePlayerQuit(player);
        }
    }
}
