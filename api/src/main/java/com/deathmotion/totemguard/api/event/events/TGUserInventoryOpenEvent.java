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

package com.deathmotion.totemguard.api.event.events;

/**
 * Fired when a user's inventory moves from closed to open. Server-driven opens are
 * transaction guarded, so the event fires when the client applies the open, not when
 * the server queued the packet. Mod-detection windows are excluded.
 */
public interface TGUserInventoryOpenEvent extends TGUserEvent {

    /**
     * {@code true} for server-driven opens ({@code OPEN_WINDOW}, including TotemGuard
     * GUIs), {@code false} for client actions inferred as an open (inventory clicks,
     * recipe book opening the crafting grid).
     */
    boolean isServerInitiated();
}
