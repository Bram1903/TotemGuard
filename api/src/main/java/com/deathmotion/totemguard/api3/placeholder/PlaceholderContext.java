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

package com.deathmotion.totemguard.api3.placeholder;

import com.deathmotion.totemguard.api3.check.Check;
import com.deathmotion.totemguard.api3.user.TGUser;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Context provided to {@link PlaceholderHolder}s during placeholder resolution.
 *
 * <p>
 * All fields are optional and depend on the call site.
 * Implementations must handle {@code null} values gracefully.
 *
 * <p>
 * The {@link #extras()} map contains call-specific values supplied by TotemGuard
 * (for example, alert-related data such as violation counts).
 * Keys and value types are not fixed and should be accessed defensively.
 */
public record PlaceholderContext(
        @Nullable TGUser user,
        @Nullable Check check,
        Map<String, Object> extras
) {

    /**
     * Creates a context without any extra values.
     *
     * @param user  user context, or {@code null}
     * @param check check context, or {@code null}
     */
    public PlaceholderContext(@Nullable TGUser user, @Nullable Check check) {
        this(user, check, Map.of());
    }

    /**
     * Retrieves a typed value from {@link #extras()}.
     *
     * @param key  extra key
     * @param type expected type
     * @return the value if present and of the requested type, otherwise {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> @Nullable T extra(String key, Class<T> type) {
        Object value = extras.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
}



