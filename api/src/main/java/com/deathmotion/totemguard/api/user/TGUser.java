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

package com.deathmotion.totemguard.api.user;

import com.deathmotion.totemguard.api.history.HistoryView;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a user within TotemGuard.
 */
public interface TGUser {
    /**
     * Returns the unique identifier (UUID) of this user.
     *
     * @return the UUID of the user.
     */
    @NotNull UUID getUuid();

    /**
     * Returns the username of this user.
     *
     * @return the user's name as a non-null string.
     */
    @NotNull String getName();

    /**
     * Returns whether this user has alerts enabled.
     *
     * @return whether this user has alerts enabled.
     */
    boolean hasAlertsEnabled();

    /**
     * Toggles the alert status for this user.
     *
     * @return the new alert status after toggling.
     */
    boolean toggleAlerts();

    /**
     * Shortcut to this user's paginated alert and punishment history. Equivalent to
     * {@code TotemGuard.getApi().getHistoryRepository().of(this)} but spares the caller
     * from threading the UUID through.
     *
     * @return a {@link HistoryView} bound to this user; never {@code null}.
     */
    @NotNull HistoryView getHistory();

    /**
     * Returns this user's ban-animation handle. A small controller that lets
     * callers play the animation and inspect its support and duration.
     *
     * @return the ban-animation handle bound to this user. Never {@code null}.
     */
    @NotNull BanAnimation getBanAnimation();
}
