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

package com.deathmotion.totemguard.bukkit.player;

import com.deathmotion.totemguard.common.platform.player.AbstractPlatformUserFactory;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BukkitPlatformUserFactory extends AbstractPlatformUserFactory<Player> {

    @Override
    protected Player getNativePlayer(@NotNull UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    @Override
    protected @NotNull PlatformUser createPlatformUser(@NotNull Player nativePlayer) {
        return new BukkitPlatformUser(nativePlayer);
    }

    @Override
    protected @Nullable PlatformPlayer createPlatformPlayer(@NotNull Player nativePlayer) {
        return new BukkitPlatformPlayer(nativePlayer);
    }
}
