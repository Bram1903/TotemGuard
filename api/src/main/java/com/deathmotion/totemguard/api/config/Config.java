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

import com.deathmotion.totemguard.api.config.key.ConfigKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory view of a YAML configuration file. Reads are thread-safe and return the
 * snapshot's value at call time, reloading swaps the snapshot atomically so callers
 * should not cache across reloads. Typed-key reads fall back to the bundled-default
 * resource on miss, path-string reads return empty on miss.
 */
public interface Config {

    /**
     * The {@link ConfigFile} this snapshot was parsed from, identifies which file on disk it represents.
     */
    @NotNull ConfigFile file();

    /**
     * Numeric schema version from the {@code config-version} root field. Drives the
     * migration pipeline, returns {@code 0} when the field is absent.
     */
    int version();

    /**
     * Whether the dot-separated {@code path} resolves to any value (including explicit
     * {@code null}) in this snapshot.
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
     * Unmodifiable view of this snapshot's root map. Mutation attempts throw, the map
     * shape mirrors the YAML root and may contain nested maps and lists.
     */
    @NotNull Map<@NotNull String, @NotNull Object> asMap();

    /**
     * Typed-key string read. Falls back to the bundled-default value on miss, and throws
     * {@link IllegalStateException} when neither the user file nor the bundled defaults
     * contain the key (a programming error, since every declared key must exist in the
     * bundled YAML).
     */
    @NotNull String getString(@NotNull ConfigKey<String> key);

    /**
     * Typed-key integer read, returns the user value or the bundled default. See
     * {@link #getString(ConfigKey)} for the throw-on-missing-default contract.
     */
    int getInt(@NotNull ConfigKey<Integer> key);

    /**
     * Typed-key boolean read, returns the user value or the bundled default. See
     * {@link #getString(ConfigKey)} for the throw-on-missing-default contract.
     */
    boolean getBoolean(@NotNull ConfigKey<Boolean> key);

    /**
     * Typed-key string-list read, returns the user value or the bundled default as an
     * unmodifiable list. See {@link #getString(ConfigKey)} for the throw-on-missing-default
     * contract.
     */
    @NotNull List<@NotNull String> getStringList(@NotNull ConfigKey<List<String>> key);
}
