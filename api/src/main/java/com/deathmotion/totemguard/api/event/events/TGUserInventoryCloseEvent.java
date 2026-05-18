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
 * Fired when a user's inventory moves from open to closed. Server-driven closes are
 * transaction guarded, so the event fires when the client applies the close.
 * Mod-detection windows are excluded. Caveat, when an inventory check fails with
 * mitigations disabled TotemGuard flips its tracker to closed to suppress repeat
 * flagging, firing this event even though the client still shows the inventory open.
 */
public interface TGUserInventoryCloseEvent extends TGUserEvent {

    /**
     * {@code true} for server-side closes (outgoing {@code CLOSE_WINDOW} or the
     * mitigations-disabled tracker flip above), {@code false} for client-sent closes.
     */
    boolean isServerInitiated();
}
