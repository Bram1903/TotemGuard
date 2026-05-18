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

package com.deathmotion.totemguard.api.placeholder;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context passed to {@link PlaceholderHolder}s during resolution. {@code user} and
 * {@code check} are call-site dependent and may be {@code null}. {@link #extras()} holds
 * call-specific values from TotemGuard (e.g. violation counts) with no fixed schema.
 *
 * @param user   user bound to this resolution, or {@code null} for non-user call sites
 * @param check  check bound to this resolution, or {@code null} for non-check call sites
 * @param extras opaque per-call values (defensively copied into an unmodifiable map by the
 *               canonical constructor), keys are namespaced by the producer
 */
public record PlaceholderContext(
        @Nullable TGUser user,
        @Nullable Check check,
        @NotNull Map<String, Object> extras
) {

    private static final PlaceholderContext EMPTY = new PlaceholderContext(null, null, Map.of());

    public PlaceholderContext {
        if (extras.isEmpty()) {
            extras = Map.of();
        } else {
            // Defensively copy only when the caller actually passed content.
            extras = Collections.unmodifiableMap(new LinkedHashMap<>(extras));
        }
    }

    /**
     * Creates a context with no extras.
     */
    public PlaceholderContext(@Nullable TGUser user, @Nullable Check check) {
        this(user, check, Map.of());
    }

    /**
     * Shared empty instance, no user, no check, no extras.
     */
    public static @NotNull PlaceholderContext empty() {
        return EMPTY;
    }

    /**
     * Retrieves a typed value from {@link #extras()}, or {@code null} if absent or wrong type.
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T extra(@NotNull String key, @NotNull Class<T> type) {
        Object value = extras.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
}

