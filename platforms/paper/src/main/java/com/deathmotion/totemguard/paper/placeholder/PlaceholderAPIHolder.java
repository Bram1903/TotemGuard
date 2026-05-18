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

package com.deathmotion.totemguard.paper.placeholder;

import com.deathmotion.totemguard.api.placeholder.PlaceholderContext;
import com.deathmotion.totemguard.api.placeholder.PlaceholderHolder;
import com.deathmotion.totemguard.paper.player.PaperPlatformPlayer;
import com.deathmotion.totemguard.common.platform.player.PlatformPlayer;
import com.deathmotion.totemguard.common.player.TGPlayer;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public final class PlaceholderAPIHolder implements PlaceholderHolder {

    @Override
    public @Nullable String resolve(@NonNull String key, @NonNull PlaceholderContext context) {
        Player player = null;

        if (context.user() instanceof TGPlayer tgPlayer) {
            PlatformPlayer platformPlayer = tgPlayer.getPlatformPlayer();
            if (platformPlayer instanceof PaperPlatformPlayer paperPlayer) {
                player = paperPlayer.getPaperPlayer();
            }
        }

        String placeholder = "%" + key + "%";
        String resolved = PlaceholderAPI.setPlaceholders(player, placeholder);
        return placeholder.equals(resolved) ? null : resolved;
    }
}
