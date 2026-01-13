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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a subsection of a {@link Config} at a specific path.
 * <p>
 * Sections provide the same read helpers as {@link Config}, but are scoped to the section root.
 */
public interface ConfigSection {

    /**
     * Returns whether a value exists at the given relative path.
     *
     * @param path dot-separated path relative to this section
     * @return true if present, otherwise false
     */
    boolean contains(String path);

    /**
     * Reads the raw value at the given relative path.
     *
     * @param path dot-separated path relative to this section
     * @return the value if present
     */
    Optional<Object> get(String path);

    /**
     * Reads a value as a string.
     *
     * @param path dot-separated path relative to this section
     * @return the string value if present
     */
    Optional<String> getString(String path);

    /**
     * Reads a value as an integer.
     *
     * @param path dot-separated path relative to this section
     * @return the integer value if present and parseable
     */
    Optional<Integer> getInt(String path);

    /**
     * Reads a value as a boolean.
     *
     * @param path dot-separated path relative to this section
     * @return the boolean value if present and parseable
     */
    Optional<Boolean> getBoolean(String path);

    /**
     * Reads a value as a double.
     *
     * @param path dot-separated path relative to this section
     * @return the double value if present and parseable
     */
    Optional<Double> getDouble(String path);

    /**
     * Reads a value as a list of strings.
     * <p>
     * If the value is missing, not a list, or an empty list, an empty list is returned.
     *
     * @param path dot-separated path relative to this section
     * @return an immutable list of strings (possibly empty)
     */
    List<String> getStringList(String path);

    /**
     * Returns a nested section if the value at the path is a map/object.
     *
     * @param path dot-separated path relative to this section
     * @return the nested section if present and a map
     */
    Optional<ConfigSection> getSection(String path);

    /**
     * Returns an unmodifiable view of the underlying map for this section.
     *
     * @return the section map
     */
    Map<String, Object> asMap();
}
