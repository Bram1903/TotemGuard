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

import com.deathmotion.totemguard.api.config.key.ConfigValueKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an in-memory view of a YAML configuration file.
 * <p>
 * Implementations should be safe to use across threads and optimized for frequent reads.
 */
public interface Config {

    /**
     * Returns which configuration file this view belongs to.
     *
     * @return the config file identifier
     */
    ConfigFile file();

    /**
     * Returns the configuration version loaded from disk (after migrations).
     *
     * @return the config version
     */
    int version();

    /**
     * Returns whether a value exists at the given path.
     *
     * @param path dot-separated path (e.g. {@code alerts.message})
     * @return true if present, otherwise false
     */
    boolean contains(String path);

    /**
     * Reads the raw value at the given path.
     *
     * @param path dot-separated path
     * @return the raw value if present
     */
    Optional<Object> get(String path);

    /**
     * Reads a value as a string.
     *
     * @param path dot-separated path
     * @return the string value if present
     */
    Optional<String> getString(String path);

    /**
     * Reads a value as an integer.
     *
     * @param path dot-separated path
     * @return the integer value if present and parseable
     */
    Optional<Integer> getInt(String path);

    /**
     * Reads a value as a boolean.
     *
     * @param path dot-separated path
     * @return the boolean value if present and parseable
     */
    Optional<Boolean> getBoolean(String path);

    /**
     * Reads a value as a double.
     *
     * @param path dot-separated path
     * @return the double value if present and parseable
     */
    Optional<Double> getDouble(String path);

    /**
     * Reads a value as a list of strings.
     * <p>
     * If the value is missing, not a list, or an empty list, an empty list is returned.
     *
     * @param path dot-separated path
     * @return an immutable list of strings (possibly empty)
     */
    List<String> getStringList(String path);

    /**
     * Returns a section if the value at the path is a map/object.
     *
     * @param path dot-separated path
     * @return the section if present and a map
     */
    Optional<ConfigSection> getSection(String path);

    /**
     * Returns an unmodifiable view of the underlying root map.
     * <p>
     * This is intended for advanced access patterns; prefer typed helpers where possible.
     *
     * @return the root map
     */
    Map<String, Object> asMap();

    /**
     * Reads a string value using a key that provides a default.
     *
     * @param key key definition containing path and default value
     * @return the configured value, or the key's default
     */
    default String getString(ConfigValueKey<String> key) {
        return getString(key.path()).orElseGet(key::defaultValue);
    }

    /**
     * Reads a string value as optional, using a key definition.
     * <p>
     * This is useful for keys that are intentionally optional.
     *
     * @param key key definition containing the path
     * @return the configured value if present
     */
    default Optional<String> getOptionalString(ConfigValueKey<String> key) {
        return getString(key.path());
    }

    /**
     * Reads an integer value using a key that provides a default.
     *
     * @param key key definition containing path and default value
     * @return the configured value, or the key's default
     */
    default int getInt(ConfigValueKey<Integer> key) {
        return getInt(key.path()).orElseGet(key::defaultValue);
    }

    /**
     * Reads a boolean value using a key that provides a default.
     *
     * @param key key definition containing path and default value
     * @return the configured value, or the key's default
     */
    default boolean getBoolean(ConfigValueKey<Boolean> key) {
        return getBoolean(key.path()).orElseGet(key::defaultValue);
    }

    /**
     * Reads a double value using a key that provides a default.
     *
     * @param key key definition containing path and default value
     * @return the configured value, or the key's default
     */
    default double getDouble(ConfigValueKey<Double> key) {
        return getDouble(key.path()).orElseGet(key::defaultValue);
    }

    /**
     * Reads a list of strings and falls back to the provided default if the list is empty.
     * <p>
     * Note: this treats "missing" and "present-but-empty" the same.
     *
     * @param key key definition containing path and default value
     * @return the configured list, or the key's default if empty
     */
    default List<String> getStringListOrDefault(ConfigValueKey<List<String>> key) {
        List<String> v = getStringList(key.path());
        return v.isEmpty() ? key.defaultValue() : v;
    }
}
