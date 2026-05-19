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

package com.deathmotion.totemguard.api.alert;

import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * In-memory toggle of staff alert subscriptions, keyed by UUID. Equivalent to
 * {@link TGUser#hasAlertsEnabled()} and {@link TGUser#toggleAlerts()} but without
 * requiring a {@code TGUser} handle. Only online UUIDs are considered, offline UUIDs
 * read as disabled and toggle as no-op.
 */
public interface AlertRepository {

    /**
     * Whether the UUID has staff alerts enabled. Returns {@code false} for offline UUIDs.
     */
    boolean hasAlertsEnabled(@NotNull UUID uuid);

    /**
     * Flips the alert state and notifies the user. No-op returning {@code false} if the
     * UUID is offline.
     *
     * @return the new alert state, or {@code false} if offline
     */
    boolean toggleAlerts(@NotNull UUID uuid);
}
