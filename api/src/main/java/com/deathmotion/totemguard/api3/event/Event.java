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

package com.deathmotion.totemguard.api3.event;

import org.jetbrains.annotations.NotNull;

/**
 * Base type for all events dispatched through the event system.
 */
public interface Event {

    /**
     * Returns the name of this event.
     * <p>
     * By default, this is the simple class name of the event implementation.
     *
     * @return the event name
     */
    default @NotNull String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Returns the timestamp associated with this event.
     * <p>
     * The value is expressed as the number of milliseconds since the Unix epoch.
     * Implementations typically use the time at which the underlying action
     * occurred. When no explicit event time is available, they may fall back to
     * the event creation time.
     *
     * @return the event timestamp in milliseconds since the Unix epoch
     */
    long getTimestamp();
}
