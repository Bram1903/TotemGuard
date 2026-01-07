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
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Context exposed to external PlaceholderHolders.
 * <p>
 * Everything is optional; depending on the call site, values may be null/missing.
 * Use extras() for extensibility (without breaking the API).
 */
public record PlaceholderContext(
        @Nullable TGUser user,
        @Nullable Check check,
        Map<String, Object> extras
) {
    public PlaceholderContext(@Nullable TGUser user, @Nullable Check check) {
        this(user, check, Map.of());
    }

    @SuppressWarnings("unchecked")
    public <T> @Nullable T extra(String key, Class<T> type) {
        Object value = extras.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
}


