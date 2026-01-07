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

import org.jetbrains.annotations.Nullable;

/**
 * Resolves custom placeholders.
 *
 * <p>
 * Implementations should return {@code null} when a key is not handled,
 * allowing other holders or built-in placeholders to resolve it.
 *
 * <p>
 * The {@code key} is provided without surrounding '%' characters.
 * For example, {@code %myplugin_rank%} will be passed as {@code "myplugin_rank"}.
 */
@FunctionalInterface
public interface PlaceholderHolder {

    /**
     * Attempts to resolve a placeholder.
     *
     * @param key     placeholder key (without '%')
     * @param context resolution context (may contain null values)
     * @return resolved value, or {@code null} if not handled
     */
    @Nullable
    String resolve(String key, PlaceholderContext context);
}


