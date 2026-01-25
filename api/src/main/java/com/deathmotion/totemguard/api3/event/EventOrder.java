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

/**
 * Defines the order in which event listeners are called when an event is fired
 * on the event bus.
 * <p>
 * Multiple listeners can listen to the same event, and this enumeration
 * determines the relative execution order among them. Listeners with earlier
 * orders are invoked before those with later ones.
 * </p>
 *
 * <p><b>Execution order:</b></p>
 * <ol>
 *   <li>{@link #FIRST} – Called before all other listeners.</li>
 *   <li>{@link #EARLY} – Called after {@code FIRST} listeners but before {@code NORMAL}.</li>
 *   <li>{@link #NORMAL} – Default listener order.</li>
 *   <li>{@link #LATE} – Called after {@code NORMAL} listeners but before {@code LAST}.</li>
 *   <li>{@link #LAST} – Called after all other listeners.</li>
 * </ol>
 */
public enum EventOrder {
    /**
     * Called before all other listeners.
     */
    FIRST,

    /**
     * Called early, before {@link #NORMAL} listeners but after {@link #FIRST}.
     */
    EARLY,

    /**
     * Default order for most listeners.
     */
    NORMAL,

    /**
     * Called late, after {@link #NORMAL} listeners but before {@link #LAST}.
     */
    LATE,

    /**
     * Called after all other listeners.
     */
    LAST
}
