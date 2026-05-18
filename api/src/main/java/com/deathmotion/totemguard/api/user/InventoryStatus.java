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
 * Snapshot of TotemGuard's view of a user's inventory. Server-driven transitions are
 * transaction guarded so the snapshot mirrors the client. Caveat, when an inventory
 * check fails with mitigations disabled TotemGuard flips this to closed to suppress
 * repeat flagging even though the player still has the inventory open client-side.
 *
 * @param open            {@code true} if TotemGuard tracks the inventory as open after the
 *                        latest transition
 * @param serverInitiated {@code true} if the last open or close was server-driven (OPEN_WINDOW,
 *                        CLOSE_WINDOW, or TotemGuard's own mitigations-disabled flip),
 *                        {@code false} for client-driven transitions
 */
public record InventoryStatus(boolean open, boolean serverInitiated) {
}
