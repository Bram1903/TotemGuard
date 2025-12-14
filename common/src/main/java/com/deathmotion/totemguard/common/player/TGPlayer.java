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

package com.deathmotion.totemguard.common.player;

import com.deathmotion.totemguard.api.event.impl.TGUserJoinEvent;
import com.deathmotion.totemguard.api.models.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.manager.PlayerManager;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUserCreation;
import com.github.retrooper.packetevents.protocol.player.User;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a player in TotemGuard. This object is bound to a single player and gets removed once the player leaves the server / proxy.
 */
public class TGPlayer implements TGUser {

    @Getter
    private final UUID uuid;
    @Getter
    private final User user;

    @Getter
    private PlatformUser platformUser;

    /**
     * Only available when the plugin is run on a backend server (so not a proxy).
     */
    @Getter
    private @Nullable PlatformPlayer platformPlayer;

    public TGPlayer(@NotNull User user) {
        this.uuid = user.getUUID();
        this.user = user;
    }

    public void onLogin() {
        TGPlatform platform = TGPlatform.getInstance();
        PlayerManager playerManager = platform.getPlayerManager();

        PlatformUserCreation platformUserCreation = platform.getPlatformUserFactory().create(uuid);
        if (platformUserCreation == null) {
            playerManager.removeUser(user);
            return;
        }

        platformUser = platformUserCreation.getPlatformUser();
        platformPlayer = platformUserCreation.getPlatformPlayer();

        if (!playerManager.shouldCheck(user, platformUser)) {
            playerManager.removeUser(user);
            return;
        }

        TGPlatform.getInstance().getEventRepository().post(new TGUserJoinEvent(this));
    }

    @Override
    public @NotNull String getName() {
        return user.getName();
    }
}
