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

import com.deathmotion.totemguard.common.player.inventory.enums.EquipmentType;
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

    @Getter
    @Setter
    private int playerWindowStateId;

    @Getter
    private int openWindowId = InventoryConstants.PLAYER_WINDOW_ID;

    @Getter
    private int openWindowTopSize = InventoryConstants.INVENTORY_SIZE;
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

    public boolean applyPickupClick(int windowId, int containerSlot, int button, Issuer issuer, long timestampMillis) {
        if (button != 0 && button != 1) {
            return false;
        }

        if (containerSlot == -999) {
            applyOutsidePickup(button, issuer, timestampMillis);
            return true;
        }

        int playerSlot = windowId == InventoryConstants.PLAYER_WINDOW_ID
                ? containerSlot
                : mapContainerSlotToPlayerSlot(windowId, containerSlot);
        if (playerSlot < 0) {
            return false;
        }

        // The player crafting area can affect multiple slots at once. Let packet payloads drive that path.
        if (windowId == InventoryConstants.PLAYER_WINDOW_ID && playerSlot <= InventoryConstants.SLOT_CRAFT_4) {
            return false;
        }

        applyPickupClickOnPlayerSlot(playerSlot, button, issuer, timestampMillis);
        return true;
    }

    public ItemStack getItem(int slot) {
        InventorySlot invSlot = slotOrNull(slot);
        if (invSlot == null) {
            return ItemStack.EMPTY;
        }

        return invSlot.getItem();
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

    public void setOpenWindow(int windowId, int topSize) {
        this.openWindowId = windowId;
        this.openWindowTopSize = topSize;
    }

    public void setOpenWindowTopSize(int topSize) {
        this.openWindowTopSize = topSize;
    }

    public void resetOpenWindow() {
        this.openWindowId = InventoryConstants.PLAYER_WINDOW_ID;
        this.openWindowTopSize = InventoryConstants.INVENTORY_SIZE;
    }

    public int mapContainerSlotToPlayerSlot(int windowId, int containerSlot) {
        if (windowId == InventoryConstants.PLAYER_WINDOW_ID) {
            return containerSlot;
        }

        if (windowId != this.openWindowId || this.openWindowTopSize < 0) {
            return -1;
        }

        int relativeSlot = containerSlot - this.openWindowTopSize;
        if (relativeSlot < 0) {
            return -1;
        }

        if (relativeSlot < 27) {
            return InventoryConstants.ITEMS_START + relativeSlot;
        }

        if (relativeSlot < 36) {
            return InventoryConstants.HOTBAR_START + (relativeSlot - 27);
        }

        return -1;
    }

    private void applyOutsidePickup(int button, Issuer issuer, long timestampMillis) {
        ItemStack currentCarried = copyItem(carriedItem.getCurrentItem());
        if (currentCarried.isEmpty()) {
            return;
        }

        if (button == 0) {
            setCarriedItem(ItemStack.EMPTY, -1, issuer, timestampMillis);
            return;
        }

        ItemStack remaining = currentCarried.copy();
        remaining.setAmount(remaining.getAmount() - 1);
        setCarriedItem(remaining.getAmount() > 0 ? remaining : ItemStack.EMPTY, -1, issuer, timestampMillis);
    }

    private void applyPickupClickOnPlayerSlot(int slot, int button, Issuer issuer, long timestampMillis) {
        ItemStack slotItem = copyItem(getItem(slot));
        ItemStack currentCarried = copyItem(carriedItem.getCurrentItem());

        if (slotItem.isEmpty()) {
            if (currentCarried.isEmpty()) {
                return;
            }

            int placeAmount = button == 0 ? currentCarried.getAmount() : 1;

            ItemStack placedItem = currentCarried.copy();
            placedItem.setAmount(placeAmount);
            setItem(slot, placedItem, issuer, SlotAction.CLICK, timestampMillis);

            if (placeAmount >= currentCarried.getAmount()) {
                setCarriedItem(ItemStack.EMPTY, slot, issuer, timestampMillis);
                return;
            }

            ItemStack remaining = currentCarried.copy();
            remaining.setAmount(currentCarried.getAmount() - placeAmount);
            setCarriedItem(remaining, slot, issuer, timestampMillis);
            return;
        }

        if (currentCarried.isEmpty()) {
            int pickupAmount = button == 0 ? slotItem.getAmount() : (slotItem.getAmount() + 1) / 2;

            ItemStack pickedUp = slotItem.copy();
            pickedUp.setAmount(pickupAmount);
            setCarriedItem(pickedUp, slot, issuer, timestampMillis);

            int remainingAmount = slotItem.getAmount() - pickupAmount;
            if (remainingAmount <= 0) {
                setItem(slot, ItemStack.EMPTY, issuer, SlotAction.CLICK, timestampMillis);
                return;
            }

            ItemStack remaining = slotItem.copy();
            remaining.setAmount(remainingAmount);
            setItem(slot, remaining, issuer, SlotAction.CLICK, timestampMillis);
            return;
        }

        if (!canPlaceInPlayerSlot(slot, currentCarried)) {
            return;
        }

        if (ItemStack.isSameItemSameTags(slotItem, currentCarried)) {
            int maxStack = getPlayerSlotMaxStack(slot, currentCarried);
            int room = maxStack - slotItem.getAmount();
            if (room <= 0) {
                return;
            }

            int insertAmount = Math.min(button == 0 ? currentCarried.getAmount() : 1, room);
            if (insertAmount <= 0) {
                return;
            }

            ItemStack updatedSlot = slotItem.copy();
            updatedSlot.setAmount(slotItem.getAmount() + insertAmount);
            setItem(slot, updatedSlot, issuer, SlotAction.CLICK, timestampMillis);

            int remainingAmount = currentCarried.getAmount() - insertAmount;
            if (remainingAmount <= 0) {
                setCarriedItem(ItemStack.EMPTY, slot, issuer, timestampMillis);
                return;
            }

            ItemStack remaining = currentCarried.copy();
            remaining.setAmount(remainingAmount);
            setCarriedItem(remaining, slot, issuer, timestampMillis);
            return;
        }

        if (currentCarried.getAmount() > getPlayerSlotMaxStack(slot, currentCarried)) {
            return;
        }

        setItem(slot, currentCarried, issuer, SlotAction.CLICK, timestampMillis);
        setCarriedItem(slotItem, slot, issuer, timestampMillis);
    }

    private boolean canPlaceInPlayerSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        return switch (slot) {
            case InventoryConstants.SLOT_CRAFT_RESULT -> false;
            case InventoryConstants.SLOT_HELMET -> EquipmentType.getEquipmentSlotForItem(stack) == EquipmentType.HEAD;
            case InventoryConstants.SLOT_CHESTPLATE ->
                    EquipmentType.getEquipmentSlotForItem(stack) == EquipmentType.CHEST;
            case InventoryConstants.SLOT_LEGGINGS -> EquipmentType.getEquipmentSlotForItem(stack) == EquipmentType.LEGS;
            case InventoryConstants.SLOT_BOOTS -> EquipmentType.getEquipmentSlotForItem(stack) == EquipmentType.FEET;
            default -> true;
        };
    }

    private int getPlayerSlotMaxStack(int slot, ItemStack stack) {
        return switch (slot) {
            case InventoryConstants.SLOT_HELMET,
                 InventoryConstants.SLOT_CHESTPLATE,
                 InventoryConstants.SLOT_LEGGINGS,
                 InventoryConstants.SLOT_BOOTS -> 1;
            default -> stack.getMaxStackSize();
        };
    }

    private ItemStack copyItem(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
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
