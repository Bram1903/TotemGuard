/*
 * This file is part of TotemGuard - https://github.com/Bram1903/TotemGuard
 * Copyright (C) 2025 Bram and contributors
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

package com.deathmotion.totemguard.common.event.internal.impl;

import com.deathmotion.totemguard.common.event.internal.InternalPlayerEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import lombok.Getter;

/**
 * Fired when a player places a new totem of undying into the offhand
 * after a previous totem was activated.
 *
 * <p>
 * This event is used to calculate the time between totem activation and
 * replenishment in order to detect automated or unrealistic behavior.
 * </p>
 */
@Getter
public class TotemReplenishedEvent extends InternalPlayerEvent {

    /**
     * Timestamp (milliseconds since epoch) when the totem was activated.
     */
    private final long totemActivatedTimestamp;

    /**
     * Timestamp (milliseconds since epoch) when the totem was replenished.
     */
    private final long totemReplenishedTimestamp;

    public TotemReplenishedEvent(
            TGPlayer player,
            long totemActivatedTimestamp,
            long totemReplenishedTimestamp
    ) {
        super(player);
        this.totemActivatedTimestamp = totemActivatedTimestamp;
        this.totemReplenishedTimestamp = totemReplenishedTimestamp;
    }
}
