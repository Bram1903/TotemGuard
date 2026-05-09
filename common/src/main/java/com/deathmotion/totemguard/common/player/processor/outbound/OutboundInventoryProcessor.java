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

package com.deathmotion.totemguard.common.player.processor.outbound;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.gui.GuiManager;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.InventoryRecipeTracker;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.inventory.enums.SlotAction;
import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.*;

import java.util.List;
import java.util.function.LongConsumer;

public class OutboundInventoryProcessor extends ProcessorOutbound {

    private final Data data;
    private final PacketInventory inventory;
    private final PacketLatencyHandler latencyHandler;
    private final GuiManager guiManager;
    private final InventoryRecipeTracker recipeTracker;

    public OutboundInventoryProcessor(TGPlayer player) {
        super(player);
        this.data = player.getData();
        this.inventory = player.getInventory();
        this.latencyHandler = player.getLatencyHandler();
        this.guiManager = TGPlatform.getInstance().getGuiManager();
        this.recipeTracker = player.getInventoryRecipeTracker();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Server.WINDOW_ITEMS) handleWindowItems(event);
        else if (type == PacketType.Play.Server.OPEN_WINDOW) handleOpenWindow(event);
        else if (type == PacketType.Play.Server.OPEN_HORSE_WINDOW) handleOpenHorseWindow(event);
        else if (type == PacketType.Play.Server.CLOSE_WINDOW) handleCloseWindow(event);
        else if (type == PacketType.Play.Server.RECIPE_BOOK_ADD) recipeTracker.handleRecipeAdd(event);
        else if (type == PacketType.Play.Server.RECIPE_BOOK_REMOVE) recipeTracker.handleRecipeRemove(event);
        else if (type == PacketType.Play.Server.RECIPE_BOOK_SETTINGS) recipeTracker.handleServerSettings(event);
        else if (type == PacketType.Play.Server.SET_PLAYER_INVENTORY) handleSetPlayerInventory(event);
        else if (type == PacketType.Play.Server.SET_SLOT) handleSetSlot(event);
        else if (type == PacketType.Play.Server.SET_CURSOR_ITEM) handleSetCursorItem(event);
        else if (type == PacketType.Play.Server.HELD_ITEM_CHANGE) handleSetHeldItem(event);
    }

    private void handleWindowItems(PacketSendEvent event) {
        WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
        final int windowId = packet.getWindowId();
        final int stateId = packet.getStateId();
        final ItemStack carried = packet.getCarriedItem().map(this::copyItemStack).orElse(null);
        final List<ItemStack> items = packet.getItems().stream().map(this::copyItemStack).toList();
        final boolean isGui = guiManager.isGuiWindow(event.getUser(), windowId);

        schedule(event, isGui, timestamp -> {
            if (carried != null) {
                inventory.setCarriedItem(carried, -1, Issuer.SERVER, timestamp);
            }

            // GUI windows are never PLAYER_WINDOW_ID; they get the external-window sync path.
            if (!isGui && windowId == InventoryConstants.PLAYER_WINDOW_ID) {
                inventory.setPlayerWindowStateId(stateId);
                inventory.resetOpenWindow();
                for (int slot = 0; slot < items.size(); slot++) {
                    inventory.setItem(slot, items.get(slot), Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
                }
                return;
            }

            if (items.size() < 36) return;

            inventory.setOpenWindow(windowId, items.size() - 36);
            syncExternalPlayerSection(windowId, items, timestamp);
        });
    }

    private void handleOpenWindow(PacketSendEvent event) {
        WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(event);
        final int containerId = packet.getContainerId();
        if (player.isModDetectionWindow(containerId)) return;
        latencyHandler.compensate(event, () -> {
            inventory.setOpenWindow(containerId, -1);
            data.setOpenInventory(true);
            data.setServerOpenedInventoryThisTick(true);
        });
    }

    private void handleOpenHorseWindow(PacketSendEvent event) {
        WrapperPlayServerOpenHorseWindow packet = new WrapperPlayServerOpenHorseWindow(event);
        final int windowId = packet.getWindowId();
        final int slotCount = packet.getSlotCount();
        latencyHandler.compensate(event, () -> {
            inventory.setOpenWindow(windowId, slotCount);
            data.setOpenInventory(true);
            data.setServerOpenedInventoryThisTick(true);
        });
    }

    private void handleCloseWindow(PacketSendEvent event) {
        WrapperPlayServerCloseWindow packet = new WrapperPlayServerCloseWindow(event);
        if (player.isModDetectionWindow(packet.getWindowId())) return;
        latencyHandler.compensate(event, timestamp -> {
            inventory.resetOpenWindow();
            // Server-side close abandons any cursor stack (placed back in inventory or dropped).
            inventory.setCarriedItem(ItemStack.EMPTY, -1, Issuer.SERVER, timestamp);
            data.setOpenInventory(false);

            if (!data.isInventoryMitigated()) return;

            data.setInventoryMitigated(false);
            data.setInventoryMitigatedThisTick(true);

            // If we initiated the close (mitigation), relay a client close-window packet to the server
            // so any server-side container (chest, etc.) also closes. Sent after the client has
            // confirmed the close to avoid compatibility issues.
            event.getUser().receivePacket(InventoryConstants.CLIENT_CLOSE_WINDOW);
        });
    }

    private void handleSetPlayerInventory(PacketSendEvent event) {
        WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
        final int containerSlot = InventoryConstants.playerInventorySlotToContainerSlot(packet.getSlot());
        if (containerSlot < 0) return;
        final ItemStack stack = copyItemStack(packet.getStack());

        latencyHandler.compensate(event, timestamp ->
                inventory.setItem(containerSlot, stack, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp));
    }

    private void handleSetSlot(PacketSendEvent event) {
        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        final int windowId = packet.getWindowId();
        if (player.isModDetectionWindow(windowId)) return;
        final int slot = packet.getSlot();
        final int stateId = packet.getStateId();
        final ItemStack item = copyItemStack(packet.getItem());
        final boolean isGui = guiManager.isGuiWindow(event.getUser(), windowId);

        schedule(event, isGui, timestamp -> applyServerSetSlot(windowId, slot, stateId, item, isGui, timestamp));
    }

    private void applyServerSetSlot(int windowId, int slot, int stateId, ItemStack item, boolean isGui, long timestamp) {
        // Legacy cursor (pre-1.21.2; replaced by SET_CURSOR_ITEM on 1.21.2+). Vanilla 1.20.4
        // ClientPacketListener#handleContainerSetSlot keys on containerId == -1 and ignores slot.
        if (windowId == -1) {
            inventory.setCarriedItem(item, -1, Issuer.SERVER, timestamp);
            return;
        }

        // Direct player-inventory write regardless of any open container, slot is in Mojang space.
        // Pre-1.21.2; replaced by SET_PLAYER_INVENTORY on 1.21.2+.
        if (windowId == -2) {
            int target = InventoryConstants.playerInventorySlotToContainerSlot(slot);
            if (target >= 0) {
                inventory.setItem(target, item, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
            }
            return;
        }

        if (!isGui && windowId == InventoryConstants.PLAYER_WINDOW_ID) {
            inventory.setPlayerWindowStateId(stateId);
            inventory.setItem(slot, item, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
            return;
        }

        int mappedSlot = inventory.mapContainerSlotToPlayerSlot(windowId, slot);
        if (mappedSlot >= 0) {
            inventory.setItem(mappedSlot, item, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
        }
    }

    private void handleSetCursorItem(PacketSendEvent event) {
        WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(event);
        final ItemStack stack = copyItemStack(packet.getStack());
        final boolean isGui = guiManager.hasSession(event.getUser());

        schedule(event, isGui, timestamp ->
                inventory.setCarriedItem(stack, -1, Issuer.SERVER, timestamp));
    }

    private void handleSetHeldItem(PacketSendEvent event) {
        WrapperPlayServerHeldItemChange packet = new WrapperPlayServerHeldItemChange(event);
        final int slot = packet.getSlot();
        if (slot < 0 || slot > 8) return;

        latencyHandler.compensate(event, () -> {
            if (inventory.getSelectedHotbarIndex() == slot) return;
            inventory.setSelectedHotbarIndex(slot);
            guiManager.refreshMonitor(player.getUuid());
        });
    }

    /**
     * GUI windows are rendered by TotemGuard itself, so state must be applied in lockstep with the
     * packet actually going out to the client (no latency compensation). Real server windows need
     * latency-compensated application so our tracker moves in sync with the client's own tick
     * that processes the packet.
     */
    private void schedule(PacketSendEvent event, boolean isGui, LongConsumer apply) {
        if (isGui) {
            trackAfterSend(event, apply);
        } else {
            latencyHandler.compensate(event, apply);
        }
    }

    private void trackAfterSend(PacketSendEvent event, LongConsumer callback) {
        if (event.isCancelled()) return;
        event.getTasksAfterSend().add(() -> callback.accept(event.getTimestamp()));
    }

    private void syncExternalPlayerSection(int windowId, List<ItemStack> items, long timestamp) {
        int playerSectionStart = items.size() - 36;
        for (int index = 0; index < 36; index++) {
            int mappedSlot = inventory.mapContainerSlotToPlayerSlot(windowId, playerSectionStart + index);
            if (mappedSlot < 0) continue;

            inventory.setItem(mappedSlot, items.get(playerSectionStart + index), Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
        }
    }

    private ItemStack copyItemStack(ItemStack itemStack) {
        return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copy();
    }
}
