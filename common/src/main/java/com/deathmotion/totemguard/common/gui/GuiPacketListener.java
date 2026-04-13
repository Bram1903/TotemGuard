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

import com.deathmotion.totemguard.common.TGPlatform;
import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.*;

public final class GuiPacketListener extends PacketListenerAbstract {

    public GuiPacketListener() {
        super(PacketListenerPriority.HIGH);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) {
            return;
        }

        GuiManager guiManager = TGPlatform.getInstance().getGuiManager();
        PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
            if (!guiManager.isGuiWindow(event.getUser(), packet.getWindowId())) {
                guiManager.trackClientWindowClick(event.getUser(), packet);
                return;
            }

            event.setCancelled(true);

            int guiSize = guiManager.windowSize(event.getUser());
            if (shouldRouteGuiAction(packet, guiSize)) {
                guiManager.handleWindowClick(event.getUser(), packet);
                return;
            }

            guiManager.resyncWindow(event.getUser(), packetFallbackCursor(packet));
            return;
        }

        if (packetType == PacketType.Play.Client.CLOSE_WINDOW) {
            WrapperPlayClientCloseWindow packet = new WrapperPlayClientCloseWindow(event);
            if (!guiManager.isGuiWindow(event.getUser(), packet.getWindowId())) {
                return;
            }

            guiManager.handleWindowClose(event.getUser(), packet.getWindowId());
            return;
        }

        if (packetType == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);
            guiManager.trackCreativeInventoryAction(event.getUser(), packet.getSlot(), packet.getItemStack());
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getConnectionState() != ConnectionState.PLAY) {
            return;
        }

        GuiManager guiManager = TGPlatform.getInstance().getGuiManager();
        PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
            guiManager.trackWindowItems(
                    event.getUser(),
                    packet.getWindowId(),
                    packet.getItems(),
                    packet.getCarriedItem().orElse(ItemStack.EMPTY)
            );
            return;
        }

        if (packetType == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
            guiManager.trackPlayerSlot(event.getUser(), packet.getSlot(), packet.getStack());
            return;
        }

        if (packetType == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
            guiManager.trackWindowSlot(event.getUser(), packet.getWindowId(), packet.getSlot(), packet.getItem());
            return;
        }

        if (packetType == PacketType.Play.Server.SET_CURSOR_ITEM) {
            WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(event);
            guiManager.trackPlayerCursor(event.getUser(), packet.getStack());
            return;
        }

        if (packetType == PacketType.Play.Server.OPEN_WINDOW) {
            WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(event);
            if (guiManager.isGuiWindow(event.getUser(), packet.getContainerId())) {
                return;
            }

            guiManager.handleExternalInventoryOpen(event.getUser(), packet.getContainerId(), -1);
            return;
        }

        if (packetType == PacketType.Play.Server.CLOSE_WINDOW) {
            WrapperPlayServerCloseWindow packet = new WrapperPlayServerCloseWindow(event);
            guiManager.handleInventoryClosePacket(event.getUser(), packet.getWindowId());
            return;
        }

        if (packetType == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            WrapperPlayServerOpenHorseWindow packet = new WrapperPlayServerOpenHorseWindow(event);
            guiManager.handleExternalInventoryOpen(event.getUser(), packet.getWindowId(), packet.getSlotCount());
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        TGPlatform.getInstance().getGuiManager().handleUserDisconnect(event.getUser().getUUID());
    }

    private boolean shouldRouteGuiAction(WrapperPlayClientClickWindow packet, int guiSize) {
        return guiSize > 0
                && packet.getWindowClickType() == WrapperPlayClientClickWindow.WindowClickType.PICKUP
                && packet.getSlot() >= 0
                && packet.getSlot() < guiSize;
    }

    private ItemStack packetFallbackCursor(WrapperPlayClientClickWindow packet) {
        ItemStack carriedItem = packet.getCarriedItemStack();
        if (carriedItem != null && !carriedItem.isEmpty()) {
            return carriedItem;
        }

        return packet.getSlots()
                .map(slots -> slots.get(packet.getSlot()))
                .filter(item -> item != null && !item.isEmpty())
                .orElse(ItemStack.EMPTY);
    }
}
