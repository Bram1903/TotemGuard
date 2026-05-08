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

package com.deathmotion.totemguard.common.features.monitor;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.slot.CarriedItem;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record MonitorSnapshot(
        @NotNull UUID targetUuid,
        @NotNull String playerName,
        @NotNull String serverName,
        @NotNull String clientVersion,
        @NotNull String clientBrand,
        int selectedHotbarIndex,
        int mainHandSlot,
        @NotNull String lastIssuer,
        boolean inventoryOpen,
        int keepAlivePing,
        int transactionPing,
        int pendingTransactionCount,
        @NotNull Map<Integer, ItemStack> inventoryItems,
        @NotNull ItemStack carriedItem,
        int carriedItemSlot,
        long capturedAtMillis
) {

    public MonitorSnapshot {
        inventoryItems = Map.copyOf(inventoryItems);
    }

    public static MonitorSnapshot captureFrom(@NotNull TGPlayer target, @NotNull String serverName) {
        PacketInventory inventory = target.getInventory();
        Map<Integer, ItemStack> items = new HashMap<>();
        for (int slot = 0; slot < InventoryConstants.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) continue;
            items.put(slot, stack.copy());
        }

        CarriedItem carried = inventory.getCarriedItem();
        ItemStack carriedStack = carried != null && carried.getCurrentItem() != null
                ? carried.getCurrentItem().copy()
                : ItemStack.EMPTY;
        int carriedSlot = carried == null ? -1 : carried.getSlot();

        return new MonitorSnapshot(
                target.getUuid(),
                target.getName() == null ? "" : target.getName(),
                serverName,
                target.getClientVersion().getReleaseName(),
                target.getClientBrand(),
                inventory.getSelectedHotbarIndex(),
                inventory.getMainHandSlot(),
                inventory.getLastIssuer().name(),
                target.getData().isOpenInventory(),
                target.getPingData().getKeepAlivePing(),
                target.getPingData().getTransactionPing(),
                target.getPingData().getPendingTransactionCount(),
                items,
                carriedStack,
                carriedSlot,
                System.currentTimeMillis()
        );
    }

    public @NotNull ItemStack itemAt(int slot) {
        ItemStack stack = inventoryItems.get(slot);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public @NotNull ItemStack mainHandItem() {
        return itemAt(mainHandSlot);
    }

    public @NotNull ItemStack offhandItem() {
        return itemAt(InventoryConstants.SLOT_OFFHAND);
    }

    /**
     * Field-by-field equality excluding {@link #capturedAtMillis}, used to skip publishing
     * heartbeat snapshots that didn't change anything material since the last tick.
     */
    public boolean contentEquals(@NotNull MonitorSnapshot other) {
        return selectedHotbarIndex == other.selectedHotbarIndex
                && mainHandSlot == other.mainHandSlot
                && inventoryOpen == other.inventoryOpen
                && keepAlivePing == other.keepAlivePing
                && transactionPing == other.transactionPing
                && pendingTransactionCount == other.pendingTransactionCount
                && carriedItemSlot == other.carriedItemSlot
                && targetUuid.equals(other.targetUuid)
                && playerName.equals(other.playerName)
                && serverName.equals(other.serverName)
                && clientVersion.equals(other.clientVersion)
                && clientBrand.equals(other.clientBrand)
                && lastIssuer.equals(other.lastIssuer)
                && Objects.equals(carriedItem, other.carriedItem)
                && inventoryItems.equals(other.inventoryItems);
    }
}
