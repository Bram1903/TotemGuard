/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.api.event.impl;

import com.deathmotion.totemguard.api.check.Check;
import com.deathmotion.totemguard.api.event.Cancellable;
import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TGAlertEvent extends TGUserEvent implements Cancellable {

    private final Check check;
    private final String debug;
    private String alertMessage;

    private boolean cancelled;

    /**
     * Creates a new alert event for the given user and check.
     *
     * @param user         the alerted user
     * @param check        the check responsible for the alert
     * @param debug        optional debug information (may be {@code null})
     * @param alertMessage the alert message to be sent to viewers with alerts enabled
     */
    public TGAlertEvent(@NotNull TGUser user,
                        @NotNull Check check,
                        @Nullable String debug,
                        @NotNull String alertMessage) {
        super(user);
        this.check = check;
        this.debug = debug;
        this.alertMessage = alertMessage;
        this.cancelled = false;
    }

    /**
     * Returns the check responsible for this alert.
     *
     * @return the check responsible for the alert
     */
    public @NotNull Check getCheck() {
        return check;
    }

    /**
     * Returns the optional debug information for this alert.
     *
     * @return the debug information, or {@code null} if none was provided
     */
    public @Nullable String getDebug() {
        return debug;
    }

    /**
     * Returns the alert message that will be sent to viewers with alerts enabled.
     *
     * @return the alert message
     */
    public @NotNull String getAlertMessage() {
        return alertMessage;
    }

    /**
     * Sets the alert message that will be sent to viewers with alerts enabled.
     *
     * @param alertMessage the new alert message
     */
    public void setAlertMessage(@NotNull String alertMessage) {
        this.alertMessage = alertMessage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
