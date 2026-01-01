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

package com.deathmotion.totemguard.common.player.inventory;

import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.enums.SlotAction;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.deathmotion.totemguard.common.player.inventory.slot.InventorySlot;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PacketInventory {

    @Getter
    private final CarriedItem carriedItem = new CarriedItem();
    @Getter
    private final Map<Integer, InventorySlot> slots;
    @Getter
    private final Set<InventorySlot> updatedSlots = new HashSet<>();
    @Getter
    @Setter
    private int selectedHotbarIndex; // 0..8
    /*
     * The last issuer that made a change to the inventory
     */
    @Getter
    @Setter
    private Issuer lastIssuer = Issuer.SERVER;

    public PacketInventory() {
        this.slots = new HashMap<>(InventoryConstants.INVENTORY_SIZE);

        for (int i = 0; i < InventoryConstants.INVENTORY_SIZE; i++) {
            this.slots.put(i, new InventorySlot(this, i));
        }
    }

    public void setCarriedItem(ItemStack carriedItem, int slot, Issuer issuer, long timestampMillis) {
        this.lastIssuer = issuer;
        this.carriedItem.update(carriedItem, slot, issuer, timestampMillis);
    }

    public ItemStack getItem(int slot) {
        InventorySlot invSlot = slotOrNull(slot);
        if (invSlot == null) {
            return ItemStack.EMPTY;
        }

        return invSlot.getItem();
    }

    public void setItem(int slot, ItemStack stack, long timestampMillis) {
        InventorySlot invSlot = slotOrNull(slot);
        if (invSlot == null) {
            return;
        }

        this.lastIssuer = Issuer.CLIENT;
        invSlot.update(stack, timestampMillis);
    }

    public void setItem(int slot, ItemStack stack, Issuer issuer, SlotAction action, long timestampMillis) {
        InventorySlot invSlot = slotOrNull(slot);
        if (invSlot == null) {
            return;
        }

        this.lastIssuer = issuer;
        invSlot.update(stack, issuer, action, timestampMillis);
    }

    public void dropItem(int slot, long timestampMillis) {
        InventorySlot invSlot = slotOrNull(slot);
        if (invSlot == null) {
            return;
        }

        this.lastIssuer = Issuer.CLIENT;
        invSlot.drop(timestampMillis);
    }

    public void dropItem(int slot, int amount, long timestampMillis) {
        InventorySlot invSlot = slotOrNull(slot);
        if (invSlot == null) {
            return;
        }

        this.lastIssuer = Issuer.CLIENT;
        invSlot.drop(amount, timestampMillis);
    }

    public int getMainHandSlot() {
        return InventoryConstants.HOTBAR_START + selectedHotbarIndex;
    }

    public ItemStack getMainHandItem() {
        return getItem(getMainHandSlot());
    }

    public ItemStack getOffhandItem() {
        return getItem(InventoryConstants.SLOT_OFFHAND);
    }

    public void swapItemToOffhand(Issuer origin, long timestamp) {
        int mainHandSlot = getMainHandSlot();

        ItemStack mainHandItem = getItem(mainHandSlot);
        ItemStack offhandItem = getItem(InventoryConstants.SLOT_OFFHAND);

        setItem(mainHandSlot, offhandItem, origin, SlotAction.SWAP, timestamp);
        setItem(InventoryConstants.SLOT_OFFHAND, mainHandItem, origin, SlotAction.SWAP, timestamp);
    }

    public void dropItemFromHand(int amount, long timestamp) {
        dropItem(getMainHandSlot(), amount, timestamp);
    }

    public void dropItemFromHand(long timestamp) {
        dropItem(getMainHandSlot(), timestamp);
    }

    public boolean isHandSlot(int slot) {
        return slot == InventoryConstants.SLOT_OFFHAND || slot == getMainHandSlot();
    }

    public boolean isTotemInSlot(int slot) {
        return getItem(slot).getType() == ItemTypes.TOTEM_OF_UNDYING;
    }

    public boolean isCarryingTotem() {
        return carriedItem.getCurrentItem().getType() == ItemTypes.TOTEM_OF_UNDYING;
    }

    private InventorySlot slot(int slot) {
        return this.slots.get(slot);
    }

    private InventorySlot slotOrNull(int slot) {
        if (slot < 0 || slot >= InventoryConstants.INVENTORY_SIZE) {
            return null;
        }
        return this.slots.get(slot);
    }
}
