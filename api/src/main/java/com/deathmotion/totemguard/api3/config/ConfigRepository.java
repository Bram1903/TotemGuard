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

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Central access point for TotemGuard's configuration.
 */
public interface ConfigRepository {

    /**
     * Returns the directory where configuration files are stored.
     *
     * @return config directory path
     */
    @NotNull Path configDirectory();

    /**
     * Returns an in-memory view of the requested configuration file.
     * <p>
     * The returned {@link Config} is expected to be safe to use across threads.
     *
     * @param file the config file to access
     * @return an in-memory config view
     */
    @NotNull Config config(@NotNull ConfigFile file);

    /**
     * Reloads a single configuration file from disk.
     * <p>
     * Implementations should apply migrations and update the in-memory view atomically.
     *
     * @param file the config file to reload
     */
    void reload(@NotNull ConfigFile file);

    /**
     * Reloads all known configuration files from disk.
     * <p>
     * Implementations should apply migrations and update in-memory views atomically.
     */
    void reloadAll();
}
