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
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final TotemGuard plugin;
    private final ConcurrentHashMap<User, TotemPlayer> playerDataMap = new ConcurrentHashMap<>();

    public PlayerDataManager(TotemGuard plugin) {
        this.plugin = plugin;
    }

    public boolean shouldCheck(User user, Player bukkitPlayer) {
        if (!ChannelHelper.isOpen(user.getChannel())) return false;
        if (user.getUUID() == null) return false;

        if (plugin.getConfigManager().getSettings().isBypass()) {
            // Has exempt permission
            if (bukkitPlayer != null && bukkitPlayer.hasPermission("TotemGuard.Bypass")) {
                return false;
            }
        }

        // Is a Geyser (Bedrock) player
        return user.getUUID().getMostSignificantBits() != 0L;
    }

    @Nullable
    public TotemPlayer getPlayer(final User user) {
        return playerDataMap.get(user);
    }

    @Nullable
    public TotemPlayer getPlayer(final Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return null;

        return playerDataMap.get(user);
    }

    public void addUser(final User user) {
        TotemPlayer player = new TotemPlayer(user);
        playerDataMap.put(user, player);
        plugin.debug("Added " + user.getName() + " to the player data map.");
    }

    public void remove(final User player) {
        playerDataMap.remove(player);
        plugin.debug("Removed " + player.getName() + " from the player data map.");
    }

    public Collection<TotemPlayer> getEntries() {
        return playerDataMap.values();
    }
}
