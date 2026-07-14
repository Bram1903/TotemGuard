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

package com.deathmotion.totemguard.api.check;

/**
 * Category a {@link Check} declares via {@code @CheckData(type = ...)}. Drives grouping
 * in alerts, history, and the in-game GUIs.
 */
public enum CheckType {

    /**
     * Fallback when a check omits {@code @CheckData(type)} or sets it to none.
     */
    UNSPECIFIED,

    /**
     * Auto-totem checks, players swapping a totem into the offhand suspiciously fast.
     */
    AUTO_TOTEM,

    /**
     * Inventory interaction checks, clicks or moves while the inventory should be closed.
     */
    INVENTORY,

    /**
     * Protocol-level checks, packets that violate the vanilla client contract.
     */
    PROTOCOL,

    /**
     * Tick-rate and tick-end timing checks. Mostly the {@code @RequiresTickEnd} family, used
     * to catch impossible tick cadences and stale-state mismatches.
     */
    TICK,

    /**
     * Client-mod fingerprinting checks driven by the mod detection subsystem.
     */
    MOD,

    /**
     * Physics and prediction checks. Motion the vanilla client could not have produced, caught by simulating the
     * reachable velocity each tick (fly, hover, ascent without support, excess speed).
     */
    PHYSICS,

    /**
     * World interaction checks. Block digging and placing the vanilla client could not have produced,
     * caught by replicating the client's digging state machine and placement rules (fast break, fast place).
     */
    WORLD
}
