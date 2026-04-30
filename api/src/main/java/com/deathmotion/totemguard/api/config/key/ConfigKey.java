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

package com.deathmotion.totemguard.api.config.key;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A typed reference to a configuration value at a specific path.
 * <p>
 * Keys carry no default value: defaults live in the bundled YAML resources shipped
 * with TotemGuard. When a value is missing on disk, implementations fall back to the
 * bundled default at lookup time.
 * <p>
 * The type parameter {@code T} is a compile-time hint that lets {@code Config.get*}
 * overloads dispatch to the correct accessor — there is no runtime type tag.
 *
 * @param <T> compile-time hint for the value type (e.g. {@link String}, {@link Integer})
 */
public final class ConfigKey<T> {

    private final String path;

    private ConfigKey(String path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public static @NotNull ConfigKey<String> string(@NotNull String path) {
        return new ConfigKey<>(path);
    }

    public static @NotNull ConfigKey<Integer> integer(@NotNull String path) {
        return new ConfigKey<>(path);
    }

    public static @NotNull ConfigKey<Boolean> bool(@NotNull String path) {
        return new ConfigKey<>(path);
    }

    public static @NotNull ConfigKey<List<String>> stringList(@NotNull String path) {
        return new ConfigKey<>(path);
    }

    public @NotNull String path() {
        return path;
    }

    @Override
    public String toString() {
        return "ConfigKey[" + path + "]";
    }
}
