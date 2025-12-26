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
import com.deathmotion.totemguard.common.player.inventory.SetSlotAction;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.Getter;

/**
 * Fired when the client explicitly sets an item in one of its inventory slots.
 * <p>
 * This event is only triggered for client-originated changes (e.g. clicks, swaps),
 * and not for server-driven updates or item removals.
 */
@Getter
public class InventoryClientSetItemEvent extends InternalPlayerEvent {

    /**
     * The inventory slot that was modified.
     */
    private final int slot;

    /**
     * The item previously in the slot.
     */
    private final ItemStack oldStack;

    /**
     * The item set by the client.
     */
    private final ItemStack newStack;

    /**
     * The action performed to set the item (e.g., click, swap).
     */
    private final SetSlotAction action;

    /**
     * Time when the change occurred, in milliseconds since epoch.
     */
    private final long timestamp;

    /**
     * Creates a new client inventory set-item event.
     *
     * @param player    the player who performed the action
     * @param slot      the modified inventory slot
     * @param oldStack  the item previously in the slot
     * @param newStack  the item set by the client
     * @param action    the action performed to set the item
     * @param timestamp time of the change in milliseconds
     */
    public InventoryClientSetItemEvent(
            TGPlayer player,
            int slot,
            ItemStack oldStack,
            ItemStack newStack,
            SetSlotAction action,
            long timestamp
    ) {
        super(player);

        this.slot = slot;
        this.oldStack = oldStack;
        this.newStack = newStack;
        this.action = action;
        this.timestamp = timestamp;
    }
}
