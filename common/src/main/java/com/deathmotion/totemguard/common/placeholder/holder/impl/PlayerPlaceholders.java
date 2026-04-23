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
import com.deathmotion.totemguard.common.placeholder.holder.MapResolverHolder;
import com.deathmotion.totemguard.common.player.TGPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

public final class PlayerPlaceholders extends MapResolverHolder<TGPlayer> {

    private static final Map<String, Function<TGPlayer, String>> RESOLVERS = Map.of(
            "tg_player", TGPlayer::getName,
            "tg_player_uuid", p -> p.getUuid().toString(),
            "tg_player_brand", TGPlayer::getClientBrand,
            "tg_player_version", p -> p.getClientVersion().getReleaseName(),
            "tg_player_ping_k", p -> String.valueOf(p.getPingData().getKeepAlivePing()),
            "tg_player_ping_t", p -> String.valueOf(p.getPingData().getTransactionPing())
    );

    public PlayerPlaceholders() {
        super(RESOLVERS);
    }

    @Override
    protected @Nullable TGPlayer subject(@NotNull InternalContext ctx) {
        return ctx.player();
    }
}
