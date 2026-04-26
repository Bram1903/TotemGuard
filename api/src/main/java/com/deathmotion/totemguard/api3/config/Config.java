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

package com.deathmotion.totemguard.api3.config;

import com.deathmotion.totemguard.api3.config.key.ConfigKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory view of a YAML configuration file.
 * <p>
 * Reads on a {@link Config} are thread-safe. Each call returns the value present in the
 * snapshot at the time of the call; reloading produces a new snapshot, so callers should
 * not cache references across reloads if they want to see updated values.
 * <p>
 * Typed-key reads ({@link #getString(ConfigKey)} etc.) fall back to the value from the
 * bundled-default YAML resource when the user file is missing the key. Path-string reads
 * ({@link #getString(String)} etc.) do not fall back; they return {@link Optional#empty()}
 * for missing values.
 */
public interface Config {

    @NotNull ConfigFile file();

    int version();

    boolean contains(@NotNull String path);

    @NotNull Optional<Object> get(@NotNull String path);

    @NotNull Optional<String> getString(@NotNull String path);

    @NotNull Optional<Integer> getInt(@NotNull String path);

    @NotNull Optional<Boolean> getBoolean(@NotNull String path);

    @NotNull List<@NotNull String> getStringList(@NotNull String path);

    @NotNull Optional<ConfigSection> getSection(@NotNull String path);

    @NotNull Map<@NotNull String, @NotNull Object> asMap();

    /**
     * Typed-key reads. Fall back to the bundled-default value if the user file does not
     * contain the key. Throw {@link IllegalStateException} if neither the user file nor
     * the bundled defaults contain the key (which indicates a programming error: every
     * declared key must exist in the bundled YAML).
     */
    @NotNull String getString(@NotNull ConfigKey<String> key);

    int getInt(@NotNull ConfigKey<Integer> key);

    boolean getBoolean(@NotNull ConfigKey<Boolean> key);

    @NotNull List<@NotNull String> getStringList(@NotNull ConfigKey<List<String>> key);
}
