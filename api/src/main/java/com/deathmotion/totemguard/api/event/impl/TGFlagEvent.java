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
import lombok.Getter;

/**
 * Represents an event fired when a {@link TGUser} is flagged by an {@link Check}.
 * <p>
 * This event is fired before the flag is fully processed and can be
 * {@link Cancellable cancelled} by listeners to prevent further handling
 */
public class TGFlagEvent extends TGUserEvent implements Cancellable {

    /**
     * The check that caused this user to be flagged.
     */
    @Getter
    private final Check check;

    /**
     * Whether this event has been canceled.
     */
    private boolean cancelled;

    /**
     * Constructs a new flag event for the given user and check.
     *
     * @param user  the user that was flagged
     * @param check the check responsible for the flag
     */
    public TGFlagEvent(TGUser user, Check check) {
        super(user);
        this.check = check;
        this.cancelled = false;
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
