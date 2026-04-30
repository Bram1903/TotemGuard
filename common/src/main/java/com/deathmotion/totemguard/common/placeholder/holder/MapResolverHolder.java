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

package com.deathmotion.totemguard.common.placeholder.holder;

import com.deathmotion.totemguard.api.placeholder.PlaceholderProvider;
import com.deathmotion.totemguard.common.placeholder.engine.InternalContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

/**
 * Base for internal placeholder holders whose resolution is a static map
 * of {@code key -> T -> String} lookups against a single subject derived
 * from the {@link InternalContext}.
 */
public abstract class MapResolverHolder<T> implements InternalPlaceholderHolder, PlaceholderProvider {

    private final Map<String, Function<T, String>> resolvers;

    protected MapResolverHolder(@NotNull Map<String, Function<T, String>> resolvers) {
        this.resolvers = resolvers;
    }

    protected abstract @Nullable T subject(@NotNull InternalContext ctx);

    @Override
    public final @NotNull Collection<String> keys() {
        return resolvers.keySet();
    }

    @Override
    public final @Nullable String resolve(@NotNull String key, @NotNull InternalContext ctx) {
        Function<T, String> fn = resolvers.get(key);
        if (fn == null) return null;

        T subject = subject(ctx);
        return subject == null ? null : fn.apply(subject);
    }
}
