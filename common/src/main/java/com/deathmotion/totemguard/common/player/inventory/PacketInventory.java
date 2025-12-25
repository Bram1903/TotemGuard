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

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.internal.impl.InventoryClientSetItemEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

public class PacketInventory {

    private final TGPlayer player;

    @Getter
    private final ItemStack[] items;

    @Getter
    @Setter
    private int selectedSlot; // 0..8

    @Getter
    @Setter
    private ItemStack carriedItem = ItemStack.EMPTY;

    public PacketInventory(TGPlayer player) {
        this.player = player;
        this.items = new ItemStack[InventoryConstants.INVENTORY_SIZE];

        for (int i = 0; i < InventoryConstants.INVENTORY_SIZE; i++) {
            items[i] = ItemStack.EMPTY;
        }
    }

    public void ResyncInventory(Optional<ItemStack> carriedItem, List<ItemStack> itemStacks) {
        this.carriedItem = carriedItem.orElse(ItemStack.EMPTY);

        for (int i = 0; i < InventoryConstants.INVENTORY_SIZE; i++) {
            items[i] = (i < itemStacks.size() && itemStacks.get(i) != null)
                    ? itemStacks.get(i)
                    : ItemStack.EMPTY;
        }
    }

    public void setItem(int slot, ItemStack stack, ChangeOrigin origin) {
        if (slot < 0 || slot >= InventoryConstants.INVENTORY_SIZE) {
            return;
        }

        ItemStack newStack = (stack == null) ? ItemStack.EMPTY : stack;
        ItemStack oldStack = items[slot];
        items[slot] = newStack;

        if (origin != ChangeOrigin.CLIENT) return;
        InventoryClientSetItemEvent event = new InventoryClientSetItemEvent(player, slot, oldStack, newStack, System.currentTimeMillis());
        TGPlatform.getInstance().getEventRepository().post(event);
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= InventoryConstants.INVENTORY_SIZE) {
            return ItemStack.EMPTY;
        }
        return items[slot];
    }

    public int getSelectedContainerSlot() {
        return InventoryConstants.HOTBAR_START + selectedSlot;
    }

    public ItemStack getMainHandItem() {
        return getItem(getSelectedContainerSlot());
    }

    public void setMainHandItem(ItemStack stack, ChangeOrigin origin) {
        setItem(getSelectedContainerSlot(), stack, origin);
    }

    public ItemStack getOffhandItem() {
        return getItem(InventoryConstants.SLOT_OFFHAND);
    }

    public void setOffhandItem(ItemStack stack, ChangeOrigin origin) {
        setItem(InventoryConstants.SLOT_OFFHAND, stack, origin);
    }

    public void swapItemToOffhand(ChangeOrigin origin) {
        int mainHandSlot = getSelectedContainerSlot();

        ItemStack mainHandItem = getItem(mainHandSlot);
        ItemStack offHandItem = getItem(InventoryConstants.SLOT_OFFHAND);

        setItem(mainHandSlot, offHandItem, origin);
        setItem(InventoryConstants.SLOT_OFFHAND, mainHandItem, origin);
    }

    public ItemStack removeItem(int slot, int amount) {
        return slot >= 0 && slot < items.length && !items[slot].isEmpty() && amount > 0
                ? items[slot].split(amount)
                : ItemStack.EMPTY;
    }

    public void removeItem(int slot) {
        if (slot >= 0 && slot < items.length) {
            items[slot] = ItemStack.EMPTY;
        }
    }

    public ItemStack removeItemFromHand(int amount) {
        return removeItem(getSelectedContainerSlot(), amount);
    }

    public void removeItemFromHand() {
        removeItem(getSelectedContainerSlot());
    }

    private boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.getType() == ItemTypes.TOTEM_OF_UNDYING;
    }
}
