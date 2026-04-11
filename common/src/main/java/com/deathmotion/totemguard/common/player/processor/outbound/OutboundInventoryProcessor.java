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
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
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

public class OutboundInventoryProcessor extends ProcessorOutbound {

    private final PacketInventory inventory;
    private final Data data;
    private final PacketLatencyHandler latencyHandler;

    public OutboundInventoryProcessor(TGPlayer player) {
        super(player);
        this.inventory = player.getInventory();
        this.data = player.getData();
        this.latencyHandler = player.getLatencyHandler();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
            if (isGuiWindow(packet.getWindowId())) {
                return;
            }

            ItemStack carriedItem = packet.getCarriedItem().map(this::copyItemStack).orElse(null);
            List<ItemStack> items = packet.getItems().stream()
                    .map(this::copyItemStack)
                    .toList();

            latencyHandler.compensate(event, timestamp -> {
                if (carriedItem != null) {
                    inventory.setCarriedItem(carriedItem, -1, Issuer.SERVER, timestamp);
                }

                if (packet.getWindowId() == InventoryConstants.PLAYER_WINDOW_ID) {
                    inventory.setPlayerWindowStateId(packet.getStateId());
                    inventory.resetOpenWindow();

                    for (int slot = 0; slot < items.size(); slot++) {
                        inventory.setItem(slot, items.get(slot), Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
                    }
                    return;
                }

                if (items.size() < 36) {
                    return;
                }

                inventory.setOpenWindow(packet.getWindowId(), items.size() - 36);
                syncExternalPlayerSection(packet.getWindowId(), items, timestamp);
            });
        } else if (packetType == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(event);

            latencyHandler.compensate(event, timestamp -> {
                inventory.setOpenWindow(packet.getContainerId(), -1);
                data.setOpenInventory(true);
                data.setServerOpenedInventoryThisTick(true);
            });
        } else if (packetType == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            WrapperPlayServerOpenHorseWindow packet = new WrapperPlayServerOpenHorseWindow(event);

            latencyHandler.compensate(event, timestamp -> {
                inventory.setOpenWindow(packet.getWindowId(), packet.getSlotCount());
                data.setOpenInventory(true);
                data.setServerOpenedInventoryThisTick(true);
            });
        } else if (packetType == PacketType.Play.Server.CLOSE_WINDOW) {
            latencyHandler.compensate(event, timestamp -> {
                inventory.resetOpenWindow();
                data.setOpenInventory(false);
            });
        } else if (packetType == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
            int slot = packet.getSlot();
            ItemStack stack = copyItemStack(packet.getStack());

            latencyHandler.compensate(event, timestamp ->
                    inventory.setItem(slot, stack, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp));
        } else if (packetType == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
            if (isGuiWindow(packet.getWindowId())) {
                return;
            }

            ItemStack item = copyItemStack(packet.getItem());

            latencyHandler.compensate(event, timestamp -> {
                if (packet.getWindowId() == InventoryConstants.PLAYER_WINDOW_ID) {
                    inventory.setPlayerWindowStateId(packet.getStateId());
                    inventory.setItem(packet.getSlot(), item, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
                    return;
                }

                int mappedSlot = inventory.mapContainerSlotToPlayerSlot(packet.getWindowId(), packet.getSlot());
                if (mappedSlot >= 0) {
                    inventory.setItem(mappedSlot, item, Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
                }
            });
        } else if (packetType == PacketType.Play.Server.SET_CURSOR_ITEM) {
            if (hasGuiSession()) {
                return;
            }

            WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(event);
            ItemStack stack = copyItemStack(packet.getStack());

            latencyHandler.compensate(event, timestamp ->
                    inventory.setCarriedItem(stack, -1, Issuer.SERVER, timestamp));
        }
    }

    private boolean hasGuiSession() {
        return TGPlatform.getInstance().getGuiManager().hasSession(player.getUser());
    }

    private boolean isGuiWindow(int windowId) {
        return TGPlatform.getInstance().getGuiManager().isGuiWindow(player.getUser(), windowId);
    }

    private void syncExternalPlayerSection(int windowId, List<ItemStack> items, long timestamp) {
        int playerSectionStart = items.size() - 36;
        for (int index = 0; index < 36; index++) {
            int mappedSlot = inventory.mapContainerSlotToPlayerSlot(windowId, playerSectionStart + index);
            if (mappedSlot < 0) {
                continue;
            }

            inventory.setItem(mappedSlot, items.get(playerSectionStart + index), Issuer.SERVER, SlotAction.IRRELEVANT, timestamp);
        }
    }

    private ItemStack copyItemStack(ItemStack itemStack) {
        return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copy();
    }
}
