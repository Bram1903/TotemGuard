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

package com.deathmotion.totemguard.common.gui;

import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.github.retrooper.packetevents.protocol.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public final class GuiViewerInventory {

    private final ItemStack[] slots = new ItemStack[InventoryConstants.INVENTORY_SIZE];
    private final boolean[] knownSlots = new boolean[InventoryConstants.INVENTORY_SIZE];
    private ItemStack cursor = ItemStack.EMPTY;
    private boolean cursorKnown;

    public synchronized void applyWindowItems(List<ItemStack> items, ItemStack carried) {
        Arrays.fill(this.slots, ItemStack.EMPTY);
        Arrays.fill(this.knownSlots, false);

        int limit = Math.min(items.size(), InventoryConstants.INVENTORY_SIZE);
        for (int slot = 0; slot < limit; slot++) {
            this.slots[slot] = copy(items.get(slot));
            this.knownSlots[slot] = true;
        }
        this.cursor = copy(carried);
        this.cursorKnown = true;
    }

    public synchronized void applySlot(int slot, ItemStack item) {
        if (slot < 0 || slot >= InventoryConstants.INVENTORY_SIZE) {
            return;
        }

        this.slots[slot] = copy(item);
        this.knownSlots[slot] = true;
    }

    public synchronized void applyCursor(ItemStack item) {
        this.cursor = copy(item);
        this.cursorKnown = true;
    }

    public synchronized boolean isKnown(int slot) {
        return slot >= 0
                && slot < InventoryConstants.INVENTORY_SIZE
                && this.knownSlots[slot];
    }

    public synchronized ItemStack item(int slot) {
        if (!isKnown(slot)) {
            return ItemStack.EMPTY;
        }

        return copy(this.slots[slot]);
    }

    public synchronized boolean isCursorKnown() {
        return this.cursorKnown;
    }

    public synchronized ItemStack cursor() {
        return copy(this.cursor);
    }

    private ItemStack copy(ItemStack item) {
        return item == null || item.isEmpty() ? ItemStack.EMPTY : item.copy();
    }
}
