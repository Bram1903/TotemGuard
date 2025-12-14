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

package com.deathmotion.totemguard.common.manager;

import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final TGPlatform platform;

    private final ConcurrentHashMap<User, TGPlayer> players = new ConcurrentHashMap<>();
    private final Collection<User> exemptUsers = ConcurrentHashMap.newKeySet();

    public PlayerManager() {
        this.platform = TGPlatform.getInstance();
    }

    public void onLoginPacket(final @NotNull User user) {
        if (!shouldCheck(user)) return;

        TGPlayer player = new TGPlayer(user);
        players.put(user, player);
    }

    public void onLogin(final @NotNull User user) {
        TGPlayer player = players.get(user);
        if (player == null) return;

        player.onLogin();
    }

    public void removeUser(final @NotNull User user) {
        players.remove(user);
    }

    public void onPlayerDisconnect(final @NotNull User user) {
        exemptUsers.remove(user);

        TGPlayer player = players.remove(user);
        if (player == null) return;

        platform.getEventRepository().post(new TGUserQuitEvent(player));
    }

    public @Nullable TGPlayer getPlayer(final @NotNull User user) {
        return players.get(user);
    }

    public boolean isExempt(User user) {
        return exemptUsers.contains(user);
    }

    public boolean shouldCheck(User user) {
        return shouldCheck(user, null);
    }

    public boolean shouldCheck(User user, @Nullable PlatformUser platformUser) {
        if (exemptUsers.contains(user)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;
        if (user.getUUID() == null) return false;

        // Is a Geyser (Bedrock) player
        if (user.getUUID().getMostSignificantBits() == 0L) {
            exemptUsers.add(user);
            return false;
        }

        if (platformUser != null && platformUser.hasPermission("TotemGuard.Bypass")) {
            exemptUsers.add(user);
            return false;
        }

        return true;
    }
}
