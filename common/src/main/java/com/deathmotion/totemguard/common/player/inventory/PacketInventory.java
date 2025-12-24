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

package com.deathmotion.totemguard.common.player.inventory;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

public class PacketInventory {
    public static final int PLAYER_WINDOW_ID = 0;
    public static final int INVENTORY_SIZE = 46;

    @Getter
    @Setter
    private int selectedSlot;

    @Getter
    @Setter
    private Optional<ItemStack> carriedItem = Optional.empty();

    @Getter
    private final ItemStack[] items;

    public PacketInventory() {
        this.items = new ItemStack[INVENTORY_SIZE];

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    public void ResyncInventory(Optional<ItemStack> carriedItem, List<ItemStack> itemStacks) {
        this.carriedItem = carriedItem;
        for (int i = 0; i < INVENTORY_SIZE && i < itemStacks.size(); i++) {
            items[i] = itemStacks.get(i);
        }
    }

    public void setItem(int slot, ItemStack stack) {
        items[slot] = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack getItem(int slot) {
        return items[slot];
    }

    public void swapItemToOffhand() {
        ItemStack mainHandItem = getItem(selectedSlot);
        ItemStack offHandItem = getItem(45);

        setItem(selectedSlot, offHandItem);
        setItem(45, mainHandItem);
    }

    public ItemStack removeItem(int slot, int amount) {
        return slot >= 0 && slot < items.length && !items[slot].isEmpty() && amount > 0 ? items[slot].split(amount) : ItemStack.EMPTY;
    }

    public void removeItem(int slot) {
        if (slot >= 0 && slot < items.length) {
            items[slot] = ItemStack.EMPTY;
        }
    }

    public ItemStack removeItemFromHand(int amount) {
        return removeItem(selectedSlot, amount);
    }

    public void removeItemFromHand() {
        removeItem(selectedSlot);
    }
}
