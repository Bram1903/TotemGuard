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
import com.deathmotion.totemguard.models.TotemPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    public final Collection<User> exemptUsers = Collections.synchronizedCollection(new HashSet<>());
    private final ConcurrentHashMap<User, TotemPlayer> playerDataMap = new ConcurrentHashMap<>();

    public boolean shouldCheck(User user) {
        if (exemptUsers.contains(user)) return false;

        UUID uuid = user.getUUID();
        if (uuid == null) return false;

        if (TotemGuard.getInstance().getConfigManager().getSettings().isBypass()) {
            // Has exempt permission
            Player player = Bukkit.getPlayer(user.getUUID());
            if (player != null &&  player.hasPermission("TotemGuard.Bypass")) {
                exemptUsers.add(user);
                return false;
            }
        }

        // Is a Geyser (Bedrock) player
        if (user.getUUID().getMostSignificantBits() == 0L) {
            return false;
        }

        return true;
    }

    @Nullable
    public TotemPlayer getPlayer(final User user) {
        return playerDataMap.get(user);
    }

    @Nullable
    public TotemPlayer getPlayer(final Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        return playerDataMap.get(user);
    }

    public void addUser(final User user) {
        if (shouldCheck(user)) {
            TotemPlayer player = new TotemPlayer(user);
            playerDataMap.put(user, player);
        }
    }

    public void remove(final User player) {
        playerDataMap.remove(player);
    }

    public Collection<TotemPlayer> getEntries() {
        return playerDataMap.values();
    }
}
