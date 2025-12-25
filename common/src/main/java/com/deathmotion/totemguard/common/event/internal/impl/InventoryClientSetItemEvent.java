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
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.Getter;

/**
 * Called when the client sets an item in their inventory (not removing items, or items set by the server).
 */
@Getter
public class InventoryClientSetItemEvent extends InternalPlayerEvent {

    private final int slot;
    private final ItemStack oldStack;
    private final ItemStack newStack;
    private final long timestampMillis;

    public InventoryClientSetItemEvent(TGPlayer player, int slot, ItemStack oldStack, ItemStack newStack, long timestampMillis) {
        super(player);

        this.slot = slot;
        this.oldStack = oldStack;
        this.newStack = newStack;
        this.timestampMillis = timestampMillis;
    }
}
