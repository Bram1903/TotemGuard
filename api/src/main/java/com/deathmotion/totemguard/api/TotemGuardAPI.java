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
 * Primary access point for the TotemGuard API.
 */
public interface TotemGuardAPI {

    /**
     * Returns the current TotemGuard version.
     *
     * @return the API version
     */
    @NotNull TGVersion getVersion();

    /**
     * Returns the event repository used for event subscription and dispatch.
     *
     * @return the event repository
     */
    @NotNull EventRepository getEventRepository();

    /**
     * Returns the configuration repository used for managing configurations.
     *
     * @return the configuration repository
     */
    @NotNull ConfigRepository getConfigRepository();

    /**
     * Returns the user repository used for user management.
     *
     * @return the user repository
     */
    @NotNull UserRepository getUserRepository();

    /**
     * Returns the placeholder repository
     *
     * @return the placeholder repository
     */
    @NotNull PlaceholderRepository getPlaceholderRepository();

    /**
     * Returns the alert repository
     *
     * @return the alert repository
     */
    @NotNull AlertRepository getAlertRepository();
}
