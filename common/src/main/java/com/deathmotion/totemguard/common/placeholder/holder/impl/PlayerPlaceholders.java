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

package com.deathmotion.totemguard.common.placeholder.holder.impl;

import com.deathmotion.totemguard.common.placeholder.engine.InternalContext;
import com.deathmotion.totemguard.common.placeholder.holder.InternalPlaceholderHolder;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.Nullable;

public final class PlayerPlaceholders implements InternalPlaceholderHolder {

    @Override
    public @Nullable String resolve(String key, InternalContext ctx) {
        TGPlayer player = ctx.player();
        if (player == null) return null;

        return switch (key) {
            case "tg_player" -> player.getName();
            case "tg_uuid" -> player.getUuid().toString();
            case "tg_client_brand" -> player.getClientBrand();
            default -> null;
        };
    }
}

