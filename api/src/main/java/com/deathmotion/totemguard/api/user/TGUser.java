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
 * A user tracked by TotemGuard.
 */
public interface TGUser {

    /**
     * The player's Minecraft UUID. Stable across sessions and across name changes.
     */
    @NotNull UUID getUuid();

    /**
     * The player's display name captured at login. Not updated mid-session, so a name
     * change won't appear until the next join.
     */
    @NotNull String getName();

    /**
     * Whether this user receives staff alerts. Mirrors the
     * {@link com.deathmotion.totemguard.api.alert.AlertRepository} state for this UUID.
     */
    boolean hasAlertsEnabled();

    /**
     * Flips the alert state and notifies the user (chat feedback) on the same call.
     *
     * @return the new alert state after the toggle
     */
    boolean toggleAlerts();

    /**
     * Shortcut to this user's paginated history, equivalent to {@code historyRepository.of(this)}.
     */
    @NotNull HistoryView getHistory();

    /**
     * Ban-animation handle bound to this user. Cheap singleton per user, safe to call
     * repeatedly. See {@link BanAnimation} for the visual contract.
     */
    @NotNull BanAnimation getBanAnimation();

    /**
     * Latency-compensated snapshot of the inventory state, mirroring the client view
     * rather than the server view.
     */
    @NotNull InventoryStatus getInventoryStatus();
}
