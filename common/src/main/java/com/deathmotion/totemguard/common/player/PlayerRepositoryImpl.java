/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2026 Bram and contributors
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

package com.deathmotion.totemguard.common.player;

import com.deathmotion.totemguard.api.event.impl.TGUserQuitEvent;
import com.deathmotion.totemguard.api.user.TGUser;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PlayerRepositoryImpl implements UserRepository {

    private static final String BYPASS_PERMISSION = "TotemGuard.Bypass";

    private final TGPlatform platform = TGPlatform.getInstance();

    private final ConcurrentMap<User, TGPlayer> players = new ConcurrentHashMap<>();
    private final Collection<UUID> exemptUsers = ConcurrentHashMap.newKeySet();

    public void onLoginPacket(final @NotNull User user) {
        if (!shouldCheck(user, null)) return;
        players.put(user, new TGPlayer(user));
    }

    public void onLogin(final @NotNull User user) {
        final UUID uuid = user.getUUID();
        final TGPlayer player = players.get(user);

        if (player != null) {
            player.onLogin();
            enableAlerts(uuid, player.getPlatformUser());
        } else {
            PlatformUserCreation platformUserCreation = platform.getPlatformUserFactory().create(uuid);
            if (platformUserCreation == null) return;
            enableAlerts(uuid, platformUserCreation.getPlatformUser());
        }
    }

    // TODO: This is temporarily (Will use a proper database implementation in the future
    private void enableAlerts(UUID uuid, PlatformUser platformUser) {
        if (platformUser.hasPermission("TotemGuard.Alerts")) {
            platform.getAlertManager().toggleAlerts(uuid);
        }
    }

    public void onPlayerDisconnect(final @NotNull User user) {
        UUID uuid = user.getUUID();
        if (uuid == null) return;

        clearExempt(uuid);

        final TGPlayer player = players.remove(user);
        if (player == null) return;

        platform.getAlertManager().removeUser(player);
        platform.getEventRepository().post(new TGUserQuitEvent(player));
    }

    public void removeUser(final @NotNull User user) {
        players.remove(user);
        clearExempt(user.getUUID());
    }

    public @Nullable TGPlayer getPlayer(final @NotNull User user) {
        return players.get(user);
    }

    public boolean isExempt(final @NotNull UUID uuid) {
        return exemptUsers.contains(uuid);
    }

    public void setExempt(final @NotNull UUID uuid, final boolean exempt) {
        if (exempt) exemptUsers.add(uuid);
        else clearExempt(uuid);
    }

    private void clearExempt(final @NotNull UUID uuid) {
        exemptUsers.remove(uuid);
    }

    public boolean shouldCheck(final @NotNull User user, final @Nullable PlatformUser platformUser) {
        final UUID uuid = user.getUUID();
        if (uuid == null) return false;

        if (isExempt(uuid)) return false;
        if (!ChannelHelper.isOpen(user.getChannel())) return false;

        if (uuid.getMostSignificantBits() == 0L) {
            setExempt(uuid, true);
            return false;
        }

        if (platformUser != null && platformUser.hasPermission(BYPASS_PERMISSION)) {
            setExempt(uuid, true);
            return false;
        }

        return true;
    }

    @Override
    public @Nullable TGUser getUser(final @NotNull UUID uuid) {
        for (User user : players.keySet()) {
            if (user.getUUID().equals(uuid)) {
                return players.get(user);
            }
        }

        return null;
    }

    public @Nullable TGPlayer getPlayer(final @NotNull UUID uuid) {
        for (User user : players.keySet()) {
            if (user.getUUID().equals(uuid)) {
                return players.get(user);
            }
        }

        return null;
    }
}
