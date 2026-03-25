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

package com.deathmotion.totemguard.bukkit.placeholder;

import com.deathmotion.totemguard.api3.placeholder.PlaceholderContext;
import com.deathmotion.totemguard.api3.placeholder.PlaceholderHolder;
import com.deathmotion.totemguard.bukkit.player.BukkitPlatformUser;
import com.deathmotion.totemguard.common.platform.player.PlatformUser;
import com.deathmotion.totemguard.common.player.TGPlayer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public final class PlaceholderAPIHolder implements PlaceholderHolder {

    @Override
    public @Nullable String resolve(@NonNull String key, @NonNull PlaceholderContext context) {
        if (context.user() == null) {
            return null;
        }

        if (!(context.user() instanceof TGPlayer tgPlayer)) return null;
        PlatformUser platformUser = tgPlayer.getPlatformUser();
        if (!(platformUser instanceof BukkitPlatformUser bukkitUser)) {
            return null;
        }

        Player player = bukkitUser.getBukkitPlayer();
        return PlaceholderAPI.setPlaceholders(player, "%" + key + "%");
    }
}
