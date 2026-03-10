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

package com.deathmotion.totemguard.api3.event.impl;

import com.deathmotion.totemguard.api3.event.Cancellable;
import com.deathmotion.totemguard.api3.user.TGUser;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an event fired when a {@link TGUser} is detected as using a VPN.
 * <p>
 * This event is fired before the VPN detection is fully processed and can be
 * {@link Cancellable cancelled} by listeners to prevent further handling.
 */
public class TGUserVPNDetectionEvent extends TGUserEvent implements Cancellable {

    private final String ip;
    private boolean cancelled;

    /**
     * Constructs a new VPN detection event for the given user and IP address.
     *
     * @param user the user that triggered the VPN detection
     * @param ip   the detected IP address
     */
    public TGUserVPNDetectionEvent(@NotNull TGUser user, @NotNull String ip) {
        super(user);
        this.ip = ip;
    }

    /**
     * Returns the IP address associated with this VPN detection.
     *
     * @return the detected IP address
     */
    public @NotNull String getIp() {
        return ip;
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