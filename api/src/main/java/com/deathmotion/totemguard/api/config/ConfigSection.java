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

package com.deathmotion.totemguard.api.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A subsection of a {@link Config} scoped to a path. Provides the same read helpers as
 * {@link Config}. All {@code path} arguments are dot-separated and relative to this section.
 */
public interface ConfigSection {

    /**
     * Whether the dot-separated {@code path} resolves to any value (including explicit
     * {@code null}) within this section.
     */
    boolean contains(@NotNull String path);

    /**
     * Raw value at {@code path} with no type coercion. Returns the YAML node as-is
     * ({@code String}, {@code Map}, {@code List}, etc.), empty when the path is missing.
     */
    @NotNull Optional<Object> get(@NotNull String path);

    /**
     * String value at {@code path}, empty when missing or the node is not a string.
     */
    @NotNull Optional<String> getString(@NotNull String path);

    /**
     * Integer value at {@code path}, empty when missing or the node is not an integer.
     */
    @NotNull Optional<Integer> getInt(@NotNull String path);

    /**
     * Boolean value at {@code path}, empty when missing or the node is not a boolean.
     */
    @NotNull Optional<Boolean> getBoolean(@NotNull String path);

    /**
     * Immutable string list at {@code path}, empty when missing or not a list. Non-string
     * elements are dropped silently.
     */
    @NotNull List<@NotNull String> getStringList(@NotNull String path);

    /**
     * Nested section view at {@code path}, empty when missing or the node is not a map.
     */
    @NotNull Optional<ConfigSection> getSection(@NotNull String path);

    /**
     * Unmodifiable view of this section's underlying map. Mutation attempts throw, nested
     * structures retain their YAML shape.
     */
    @NotNull Map<@NotNull String, @NotNull Object> asMap();
}
