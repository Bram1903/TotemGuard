/*
 *  This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 *  Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.events.lunarclient;

import com.deathmotion.totemguard.TotemGuard;
import com.deathmotion.totemguard.models.TotemPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;

public class ApolloPlayerProtoListener implements Listener {

    public ApolloPlayerProtoListener(TotemGuard plugin) {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "lunar:apollo");
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(plugin, "lunar:apollo", (s, player, bytes) -> {});
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    private void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!event.getChannel().equalsIgnoreCase("lunar:apollo")) {
            return;
        }

        TotemPlayer totemPlayer = TotemGuard.getInstance().getPlayerDataManager().getPlayer(event.getPlayer());
        if (totemPlayer == null) return;

        totemPlayer.isUsingLunarClient = true;
    }
}
