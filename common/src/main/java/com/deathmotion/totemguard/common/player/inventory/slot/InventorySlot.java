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

import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.enums.SlotAction;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.Getter;

@Getter
public class InventorySlot {

    private final PacketInventory inventory;
    private final int slot;

    private ItemStack item;
    private Issuer issuer;
    private SlotAction slotAction;
    private long updated;

    private SlotState previous;

    public InventorySlot(PacketInventory inventory, int slot) {
        this.inventory = inventory;
        this.slot = slot;

        long now = System.currentTimeMillis();
        this.item = ItemStack.EMPTY;
        this.issuer = Issuer.SERVER;
        this.slotAction = SlotAction.IRRELEVANT;
        this.updated = now;

        this.previous = new SlotState(this.item, this.issuer, this.slotAction, this.updated);
    }

    public void update(ItemStack newItem, long timestamp) {
        update(newItem, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
    }

    public void update(ItemStack newItem, Issuer issuer, SlotAction slotAction, long timestamp) {
        // Old logic: always notify on update (no dedup / no "enqueued" flag)
        this.previous = new SlotState(this.item, this.issuer, this.slotAction, this.updated);

        this.item = newItem;
        this.issuer = issuer;
        this.slotAction = slotAction;
        this.updated = timestamp;

        inventory.getUpdatedSlots().add(this);
    }

    public void drop(long timestamp) {
        update(ItemStack.EMPTY, Issuer.CLIENT, SlotAction.DROP, timestamp);
    }

    public void drop(int amount, long timestamp) {
        ItemStack newItem;
        if (this.item.getAmount() <= amount) {
            newItem = ItemStack.EMPTY;
        } else {
            ItemStack copy = this.item.copy();
            copy.setAmount(copy.getAmount() - amount);
            newItem = copy;
        }

        update(newItem, Issuer.CLIENT, SlotAction.DROP, timestamp);
    }
}
