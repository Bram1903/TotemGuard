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

package com.deathmotion.totemguard.common.player.processor.inbound;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.InventoryRecipeTracker;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.enums.SlotAction;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.*;

import java.util.Map;

public class InboundInventoryProcessor extends ProcessorInbound {

    private final GuiManager guiManager;
    private final PacketInventory inventory;
    private final Data data;
    private final InventoryRecipeTracker recipeTracker;

    public InboundInventoryProcessor(TGPlayer player) {
        super(player);

        this.guiManager = TGPlatform.getInstance().getGuiManager();
        this.inventory = player.getInventory();
        this.data = player.getData();
        this.recipeTracker = player.getInventoryRecipeTracker();
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.PLAYER_DIGGING) handleDigging(event);
        else if (type == PacketType.Play.Client.HELD_ITEM_CHANGE) handleHeldItemChange(event);
        else if (type == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) handleCreativeAction(event);
        else if (type == PacketType.Play.Client.CLICK_WINDOW) handleClickWindow(event);
        else if (type == PacketType.Play.Client.CLOSE_WINDOW) handleCloseWindow(event);
        else if (type == PacketType.Play.Client.SET_RECIPE_BOOK_STATE) handleSetRecipeBookState(event);
        else if (type == PacketType.Play.Client.SET_DISPLAYED_RECIPE) handleSetDisplayedRecipe(event);
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        PacketTypeCommon type = event.getPacketType();
        if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            WrapperPlayClientCloseWindow packet = new WrapperPlayClientCloseWindow(event);
            if (player.isModDetectionWindow(packet.getWindowId())) return;
            data.setInventoryMitigatedThisTick(false);
            return;
        }

        boolean tickBoundary = WrapperPlayClientPlayerFlying.isFlying(type) || (type == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTick());
        if (tickBoundary) {
            data.applyPendingOpenInventory();
            data.setServerOpenedInventoryThisTick(false);
        }
    }

    private void handleDigging(PacketReceiveEvent event) {
        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
        long timestamp = event.getTimestamp();
        switch (packet.getAction()) {
            case DROP_ITEM_STACK -> inventory.dropItemFromHand(timestamp);
            case DROP_ITEM -> inventory.dropItemFromHand(1, timestamp);
            case SWAP_ITEM_WITH_OFFHAND -> inventory.swapItemToOffhand(Issuer.CLIENT, timestamp);
            default -> {
            }
        }
    }

    private void handleHeldItemChange(PacketReceiveEvent event) {
        WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);
        int slot = packet.getSlot();
        if (slot < 0 || slot > 8) return;
        if (inventory.getSelectedHotbarIndex() == slot) return;

        inventory.setSelectedHotbarIndex(slot);
        guiManager.refreshMonitor(player.getUuid());
    }

    private void handleCreativeAction(PacketReceiveEvent event) {
        WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);
        int slot = packet.getSlot();
        if (slot < 1 || slot > 45) return;

        inventory.setItem(slot, copyItem(packet.getItemStack()), Issuer.CLIENT, SlotAction.IRRELEVANT, event.getTimestamp());
    }

    private void handleClickWindow(PacketReceiveEvent event) {
        WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        if (player.isModDetectionWindow(packet.getWindowId())) return;
        data.setOpenInventory(true);

        final int windowId = packet.getWindowId();
        final int containerSlot = packet.getSlot();
        final long timestamp = event.getTimestamp();
        final boolean inPlayerWindow = windowId == InventoryConstants.PLAYER_WINDOW_ID;

        final int carriedSlot = inPlayerWindow
                ? containerSlot
                : inventory.mapContainerSlotToPlayerSlot(windowId, containerSlot);

        boolean predictedPickup = packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.PICKUP
                && inventory.applyPickupClick(windowId, containerSlot, packet.getButton(), Issuer.CLIENT, timestamp);

        if (!predictedPickup) {
            inventory.setCarriedItem(copyItem(packet.getCarriedItemStack()), carriedSlot, Issuer.CLIENT, timestamp);
        }

        applyClientSlotPayload(packet, windowId, containerSlot, carriedSlot, predictedPickup, timestamp);
    }

    private void applyClientSlotPayload(WrapperPlayClientClickWindow packet,
                                        int windowId,
                                        int containerSlot,
                                        int carriedSlot,
                                        boolean predictedPickup,
                                        long timestamp) {
        packet.getSlots().ifPresent(slots -> {
            boolean inPlayerWindow = windowId == InventoryConstants.PLAYER_WINDOW_ID;
            for (Map.Entry<Integer, ItemStack> entry : slots.entrySet()) {
                int entrySlot = entry.getKey();
                int mappedSlot = inPlayerWindow
                        ? entrySlot
                        : inventory.mapContainerSlotToPlayerSlot(windowId, entrySlot);
                if (mappedSlot < 0) continue;

                // Skip the slot we already predicted to avoid double-applying it.
                if (predictedPickup && mappedSlot == carriedSlot && entrySlot == containerSlot) continue;

                inventory.setItem(mappedSlot, copyItem(entry.getValue()), Issuer.CLIENT, SlotAction.CLICK, timestamp);
            }
        });
    }

    private void handleCloseWindow(PacketReceiveEvent event) {
        WrapperPlayClientCloseWindow packet = new WrapperPlayClientCloseWindow(event);
        if (player.isModDetectionWindow(packet.getWindowId())) return;
        data.setOpenInventory(false);
        if (packet.getWindowId() == InventoryConstants.PLAYER_WINDOW_ID) {
            recipeTracker.armAfterClientClose();
        }
    }

    private void handleSetRecipeBookState(PacketReceiveEvent event) {
        WrapperPlayClientSetRecipeBookState packet = new WrapperPlayClientSetRecipeBookState(event);
        recipeTracker.recordClientState(packet.getBookType(), packet.isBookOpen(), packet.isFilterActive());
    }

    private void handleSetDisplayedRecipe(PacketReceiveEvent event) {
        if (recipeTracker.handleDisplayedRecipe(event)) {
            data.setVerifiedOpenInventory();
        }
    }

    private ItemStack copyItem(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}
