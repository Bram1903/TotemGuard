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
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
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
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = packet.getAction();

            switch (action) {
                case DROP_ITEM_STACK -> inventory.dropItemFromHand(event.getTimestamp());
                case DROP_ITEM -> inventory.dropItemFromHand(1, event.getTimestamp());
                case SWAP_ITEM_WITH_OFFHAND -> inventory.swapItemToOffhand(Issuer.CLIENT, event.getTimestamp());
            }
        } else if (packetType == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);
            int slot = packet.getSlot();
            if (slot > 8 || slot < 0) return;
            if (inventory.getSelectedHotbarIndex() == slot) return;
            inventory.setSelectedHotbarIndex(slot);
            guiManager.refreshMonitor(player.getUuid());
        } else if (packetType == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);
            if (packet.getSlot() < 1 || packet.getSlot() > 45) return;
            inventory.setItem(packet.getSlot(), copyItem(packet.getItemStack()), Issuer.CLIENT, SlotAction.IRRELEVANT, event.getTimestamp());
        } else if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
            data.setOpenInventory(true);
            int carriedSlot = packet.getWindowId() == InventoryConstants.PLAYER_WINDOW_ID
                    ? packet.getSlot()
                    : inventory.mapContainerSlotToPlayerSlot(packet.getWindowId(), packet.getSlot());
            boolean predictedPickup = packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.PICKUP
                    && inventory.applyPickupClick(packet.getWindowId(), packet.getSlot(), packet.getButton(), Issuer.CLIENT, event.getTimestamp());

            if (!predictedPickup) {
                inventory.setCarriedItem(copyItem(packet.getCarriedItemStack()), carriedSlot, Issuer.CLIENT, event.getTimestamp());
            }

            packet.getSlots().ifPresent(slots -> {
                for (Map.Entry<Integer, ItemStack> slotEntry : slots.entrySet()) {
                    int mappedSlot = packet.getWindowId() == InventoryConstants.PLAYER_WINDOW_ID
                            ? slotEntry.getKey()
                            : inventory.mapContainerSlotToPlayerSlot(packet.getWindowId(), slotEntry.getKey());

                    if (mappedSlot < 0) {
                        continue;
                    }

                    if (predictedPickup && mappedSlot == carriedSlot && slotEntry.getKey() == packet.getSlot()) {
                        continue;
                    }

                    inventory.setItem(mappedSlot, copyItem(slotEntry.getValue()), Issuer.CLIENT, SlotAction.CLICK, event.getTimestamp());
                }
            });
        } else if (packetType == PacketType.Play.Client.CLOSE_WINDOW) {
            WrapperPlayClientCloseWindow packet = new WrapperPlayClientCloseWindow(event);
            data.setOpenInventory(false);
            if (packet.getWindowId() == InventoryConstants.PLAYER_WINDOW_ID) {
                recipeTracker.armAfterClientClose();
            }
        } else if (packetType == PacketType.Play.Client.SET_RECIPE_BOOK_STATE) {
            WrapperPlayClientSetRecipeBookState packet = new WrapperPlayClientSetRecipeBookState(event);
            recipeTracker.recordClientState(packet.getBookType(), packet.isBookOpen());
        } else if (packetType == PacketType.Play.Client.SET_DISPLAYED_RECIPE) {
            if (recipeTracker.handleDisplayedRecipe(event)) {
                data.setVerifiedOpenInventory();
            }
        }
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            data.setInventoryMitigatedThisTick(false);
        } else if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType()) || (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTick())) {
            data.applyPendingOpenInventory();
            data.setServerOpenedInventoryThisTick(false);
        }
    }

    private ItemStack copyItem(ItemStack stack) {
        return stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }
}
