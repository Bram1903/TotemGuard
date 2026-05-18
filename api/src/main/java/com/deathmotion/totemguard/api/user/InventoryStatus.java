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

package com.deathmotion.totemguard.api.user;

/**
 * Snapshot of how TotemGuard sees a user's inventory. Returned by
 * {@link TGUser#getInventoryStatus()}. Server-driven transitions are
 * transaction guarded, so the snapshot mirrors the client's view.
 * <p>
 * Caveat: when an inventory check fails with mitigations disabled,
 * TotemGuard flips this to closed to suppress repeat flagging even
 * though the player still has the inventory open on their screen. With
 * mitigations enabled the close is real and guarded.
 *
 * @param open            {@code true} when TotemGuard tracks the inventory as open
 * @param serverInitiated {@code true} when the last transition came from
 *                        the server side (including TotemGuard itself)
 */
public record InventoryStatus(boolean open, boolean serverInitiated) {
}
