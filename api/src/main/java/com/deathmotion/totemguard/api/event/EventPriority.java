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

package com.deathmotion.totemguard.api.event;

/**
 * Pre-defined priority slots for the priority-aware subscribe overloads. Smaller numbers
 * run earlier, larger numbers run later and decide the final cancellation state. Any
 * {@code int} works, these constants are a shared vocabulary.
 */
public final class EventPriority {

    /**
     * Earliest slot, for handlers that want to observe (or veto) an event before anyone else.
     */
    public static final int LOWEST = -200;

    /**
     * Between {@link #LOWEST} and {@link #NORMAL}, room for early pre-processing handlers.
     */
    public static final int LOW = -100;

    /**
     * Default slot when the no-priority {@code subscribe} overload is used.
     */
    public static final int NORMAL = 0;

    /**
     * Between {@link #NORMAL} and {@link #HIGHEST}, room for late post-processing handlers.
     */
    public static final int HIGH = 100;

    /**
     * Last slot among handlers that can mutate cancellation state. Use this when your
     * handler needs to override decisions made by other handlers.
     */
    public static final int HIGHEST = 200;

    /**
     * Observe-only slot for metrics, audit, and logging. Runs after every cancellation-mutating
     * handler so observers see the final state, must not mutate the event.
     */
    public static final int MONITOR = 300;

    private EventPriority() {
    }
}
