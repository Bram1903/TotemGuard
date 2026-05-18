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
 * In-memory toggle of staff alert subscriptions, keyed by UUID. Offers the same toggle
 * surface as {@link TGUser#hasAlertsEnabled()} / {@link TGUser#toggleAlerts()} but
 * without first requiring a {@code TGUser} handle, which is useful when all you have is
 * a UUID from a paper event.
 * <p>
 * Both methods only consider <em>online</em> staff: a UUID that is not currently in
 * memory is treated as "alerts disabled". Toggling for an offline UUID does nothing
 * and returns {@code false}.
 */
public interface AlertRepository {

    /**
     * Whether the given UUID currently has staff alerts enabled.
     *
     * @param uuid the UUID to check; must be online for the answer to be meaningful
     * @return {@code true} if alerts are enabled, {@code false} otherwise (including
     * the offline case)
     */
    boolean hasAlertsEnabled(@NotNull UUID uuid);

    /**
     * Flips the alert state for the given UUID and notifies the user. No-op if the
     * UUID is not currently online.
     *
     * @param uuid the UUID to toggle
     * @return the new alert state, or {@code false} if the UUID is not online
     */
    boolean toggleAlerts(@NotNull UUID uuid);
}
