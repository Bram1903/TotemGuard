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

package com.deathmotion.totemguard.api;

import com.deathmotion.totemguard.api.alert.AlertRepository;
import com.deathmotion.totemguard.api.config.ConfigRepository;
import com.deathmotion.totemguard.api.event.EventRepository;
import com.deathmotion.totemguard.api.placeholder.PlaceholderRepository;
import com.deathmotion.totemguard.api.user.UserRepository;
import com.deathmotion.totemguard.api.versioning.TGVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Primary entry point for accessing the TotemGuard API.
 */
public interface TotemGuardAPI {

    /**
     * Returns the current TotemGuard API version.
     *
     * @return the API version, never {@code null}
     */
    @NotNull TGVersion getVersion();

    /**
     * Returns the repository for event subscription and dispatch.
     *
     * @return the event repository, never {@code null}
     */
    @NotNull EventRepository getEventRepository();

    /**
     * Returns the repository for configuration management.
     *
     * @return the configuration repository, never {@code null}
     */
    @NotNull ConfigRepository getConfigRepository();

    /**
     * Returns the repository for user management.
     *
     * @return the user repository, never {@code null}
     */
    @NotNull UserRepository getUserRepository();

    /**
     * Returns the repository for placeholder management.
     *
     * @return the placeholder repository, never {@code null}
     */
    @NotNull PlaceholderRepository getPlaceholderRepository();

    /**
     * Returns the repository for alert management.
     *
     * @return the alert repository, never {@code null}
     */
    @NotNull AlertRepository getAlertRepository();
}
