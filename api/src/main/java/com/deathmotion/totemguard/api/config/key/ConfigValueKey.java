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

import java.util.Objects;

/**
 * Defines a configuration value at a specific path, optionally with a default value.
 * <p>
 * This is intended to centralize configuration paths and fallback values,
 * keeping call-sites clean and consistent.
 *
 * @param <T> value type (e.g. {@link String}, {@link Integer})
 */
public final class ConfigValueKey<T> {

    private final String path;
    private final T defaultValue;
    private final boolean hasDefault;

    private ConfigValueKey(String path, T defaultValue, boolean hasDefault) {
        this.path = Objects.requireNonNull(path, "path");
        this.defaultValue = defaultValue;
        this.hasDefault = hasDefault;
    }

    /**
     * Creates a key with a required default value (used as fallback).
     *
     * @param path         dot-separated config path
     * @param defaultValue fallback value
     * @param <T>          value type
     * @return the key instance
     */
    public static <T> ConfigValueKey<T> required(String path, T defaultValue) {
        return new ConfigValueKey<>(
                path,
                Objects.requireNonNull(defaultValue, "defaultValue"),
                true
        );
    }

    /**
     * Creates a key with no default value.
     * <p>
     * Consumers should read it as optional via API helpers such as
     * {@code Config#getOptionalString(ConfigValueKey)}.
     *
     * @param path dot-separated config path
     * @param <T>  value type
     * @return the key instance
     */
    public static <T> ConfigValueKey<T> optional(String path) {
        return new ConfigValueKey<>(path, null, false);
    }

    /**
     * Returns the dot-separated config path.
     *
     * @return config path
     */
    public String path() {
        return path;
    }

    /**
     * Returns whether this key has a default value.
     *
     * @return true if a default is present
     */
    public boolean hasDefault() {
        return hasDefault;
    }

    /**
     * Returns the default value.
     *
     * @return default value
     * @throws IllegalStateException if no default is defined
     */
    public T defaultValue() {
        if (!hasDefault) {
            throw new IllegalStateException("Key has no default: " + path);
        }
        return defaultValue;
    }
}
