/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2024 Bram and contributors
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

package com.deathmotion.totemguard.api;

import com.deathmotion.totemguard.api.interfaces.IAlertManager;
import com.deathmotion.totemguard.api.interfaces.IConfigManager;
import com.deathmotion.totemguard.api.versioning.TGVersion;

import java.util.Optional;

/**
 * This is the main API class for TotemGuard.
 */
public interface ITotemGuardAPI {

    /**
     * Check if the API is enabled.
     *
     * @return True if the API is enabled, false otherwise.
     */
    boolean isEnabled();

    /**
     * Get the name of the server.
     *
     * @return The name of the server.
     */
    String getServerName();

    /**
     * Get the version of TotemGuard.
     *
     * @return The version of TotemGuard.
     */
    TGVersion getVersion();

    /**
     * Get the latest version of TotemGuard.
     *
     * @return An Optional containing the latest version of TotemGuard if available, or an empty Optional otherwise.
     */
    Optional<TGVersion> getLatestVersion();

    /**
     * Get the alert manager.
     *
     * @return The alert manager.
     */
    IAlertManager getAlertManager();

    /**
     * Get the config manager.
     *
     * @return The config manager.
     */
    IConfigManager getConfigManager();
}
