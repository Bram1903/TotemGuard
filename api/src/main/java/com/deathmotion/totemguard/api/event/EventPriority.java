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
 * Pre-defined priority slots that callers can pass into the priority-aware
 * subscribe overloads.
 * <p>
 * Smaller numbers run earlier in the dispatch chain. Larger numbers run later
 * and therefore decide the final cancellation state. Any {@code int} works,
 * the constants here just give plugins a shared vocabulary so two unrelated
 * plugins can order against each other without inventing arbitrary values.
 */
public final class EventPriority {

    /**
     * Runs first, ahead of every other priority.
     */
    public static final int LOWEST = -200;

    /**
     * Sits between {@link #LOWEST} and {@link #NORMAL}.
     */
    public static final int LOW = -100;

    /**
     * The default slot, used when no priority is passed.
     */
    public static final int NORMAL = 0;

    /**
     * Sits between {@link #NORMAL} and {@link #HIGHEST}.
     */
    public static final int HIGH = 100;

    /**
     * Runs after the regular priorities. Use this when a handler needs the last word on cancellation.
     */
    public static final int HIGHEST = 200;

    /**
     * Reserved for read-only observers such as metrics or audit logging. Runs after everything else.
     */
    public static final int MONITOR = 300;

    private EventPriority() {
    }
}
