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

package com.deathmotion.totemguard.common.player.inventory.slot;

import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.enums.SlotAction;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.Getter;

@Getter
public class CarriedItem {
    private ItemStack currentItem = ItemStack.EMPTY;
    private int slot = -1;
    private Issuer issuer = Issuer.SERVER;
    private long timestamp = System.currentTimeMillis();
    private boolean isUpdated;

    private SlotState previous;

    public void update(ItemStack newItem, int slot, Issuer issuer, long timestamp) {
        if (newItem.equals(this.currentItem)) return;

        this.previous = new SlotState(this.currentItem, issuer, SlotAction.IRRELEVANT, this.timestamp);

        this.currentItem = newItem;
        this.slot = slot;
        this.issuer = issuer;
        this.timestamp = timestamp;
        this.isUpdated = true;
    }

    public void hasUpdated() {
        this.isUpdated = false;
    }
}
