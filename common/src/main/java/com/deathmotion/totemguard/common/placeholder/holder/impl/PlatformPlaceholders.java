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

import com.deathmotion.totemguard.api3.config.ConfigFile;
import com.deathmotion.totemguard.common.config.key.MessagesKeys;
import com.deathmotion.totemguard.common.placeholder.engine.InternalContext;
import com.deathmotion.totemguard.common.placeholder.holder.MapResolverHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public final class PlatformPlaceholders extends MapResolverHolder<InternalContext> {

    private static final Map<String, Function<InternalContext, String>> RESOLVERS = Map.of(
            "prefix", ctx -> ctx.platform()
                    .getConfigRepository()
                    .config(ConfigFile.MESSAGES)
                    .getString(MessagesKeys.PREFIX),
            "tg_server", ctx -> ctx.platform()
                    .getConfigRepository()
                    .configView()
                    .server()
    );

    public PlatformPlaceholders() {
        super(RESOLVERS);
    }

    @Override
    protected @NotNull InternalContext subject(@NotNull InternalContext ctx) {
        return ctx;
    }
}
