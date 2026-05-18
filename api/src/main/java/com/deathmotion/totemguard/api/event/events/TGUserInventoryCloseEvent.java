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
 * Fired when TotemGuard sees a user's inventory move from open to closed.
 * Server-driven closes are transaction guarded, so the event fires when
 * the client actually applies the close. Mod-detection windows are
 * excluded.
 * <p>
 * Caveat: when an inventory check fails on a user and mitigations are
 * disabled, TotemGuard flips its tracker to closed to suppress repeat
 * flagging even though no close packet went out. This event fires in
 * that case, even though the player's screen still shows the inventory
 * open. With mitigations enabled the close is real and guarded.
 */
public interface TGUserInventoryCloseEvent extends TGUserEvent {

    /**
     * {@code true} when the close was driven by the server side (an
     * outgoing {@code CLOSE_WINDOW} packet, or a TotemGuard-internal
     * close from the caveat above). {@code false} when the client sent
     * the close packet itself.
     */
    boolean isServerInitiated();
}
