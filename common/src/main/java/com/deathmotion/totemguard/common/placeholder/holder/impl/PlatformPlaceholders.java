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

import com.deathmotion.totemguard.api.placeholder.PlaceholderProvider;
import com.deathmotion.totemguard.common.placeholder.engine.InternalContext;
import com.deathmotion.totemguard.common.placeholder.holder.InternalPlaceholderHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public final class PlatformPlaceholders implements InternalPlaceholderHolder, PlaceholderProvider {

    private static final Map<String, Function<InternalContext, String>> RESOLVERS;
    static {
        RESOLVERS = Map.of("prefix", ctx -> ctx.platform()
                .getConfigRepository()
                .messages()
                .node("prefix")
                .getString("&6&lTG &8»"));
    }

    @Override
    public @NotNull Collection<String> keys() {
        return RESOLVERS.keySet();
    }

    @Override
    public @Nullable String resolve(String key, InternalContext ctx) {
        Function<InternalContext, String> fn = RESOLVERS.get(key);
        return fn != null ? fn.apply(ctx) : null;
    }
}


