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

import com.deathmotion.totemguard.api.event.Cancellable;

/**
 * Fired the moment TotemGuard is about to rubber-band a player back to a legal position,
 * whether the correction came from the movement engine (a fly, climb, speed, or phase that
 * failed the simulation) or a non-movement check (a positive timer). Cancelling aborts the
 * correction entirely, the player keeps their reported position, so the check still alerts
 * but the advantage is not removed. This is the per-player {@code nosetback} hook.
 * <p>
 * Fired on the packet-handling thread for the player, synchronously, before the correction
 * packet or teleport is sent. Coordinates are absolute world coordinates. The from-position
 * is where the player currently is (the illegal position), the to-position is where the
 * correction would place them.
 */
public interface TGSetbackEvent extends TGUserEvent, Cancellable {

    /**
     * X of the position the player is being pulled back from (the current, illegal position).
     */
    double getFromX();

    /**
     * Y of the position the player is being pulled back from (the current, illegal position).
     */
    double getFromY();

    /**
     * Z of the position the player is being pulled back from (the current, illegal position).
     */
    double getFromZ();

    /**
     * X of the position the player is being pulled back to (the legal target).
     */
    double getToX();

    /**
     * Y of the position the player is being pulled back to (the legal target).
     */
    double getToY();

    /**
     * Z of the position the player is being pulled back to (the legal target).
     */
    double getToZ();

    /**
     * Whether the correction is delivered as a direct client position packet ({@code true},
     * used for small setbacks within a few blocks) or as an authoritative server teleport
     * ({@code false}, used for large setbacks that would otherwise trip the vanilla
     * "moved too quickly" check).
     */
    boolean isPacketCorrection();
}
