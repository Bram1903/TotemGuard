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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.Data;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.inventory.enums.Issuer;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;

public class OutboundInventoryProcessor extends ProcessorOutbound {

    private final PacketInventory inventory;
    private final Data data;

    public OutboundInventoryProcessor(TGPlayer player) {
        super(player);
        this.inventory = player.getInventory();
        this.data = player.getData();
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);
            if (packet.getWindowId() != InventoryConstants.PLAYER_WINDOW_ID) return;
            packet.getCarriedItem().ifPresent(carriedItem -> inventory.setCarriedItem(carriedItem, -1, Issuer.SERVER, event.getTimestamp()));
            for (int slot = 0; slot < packet.getItems().size(); slot++) {
                inventory.setItem(slot, packet.getItems().get(slot), event.getTimestamp());
            }
        } else if (packetType == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(event);
            inventory.setItem(packet.getSlot(), packet.getStack(), event.getTimestamp());
        } else if (packetType == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
            if (packet.getWindowId() != InventoryConstants.PLAYER_WINDOW_ID) return;
            inventory.setItem(packet.getSlot(), packet.getItem(), event.getTimestamp());
        } else if (packetType == PacketType.Play.Server.SET_CURSOR_ITEM) {
            WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(event);
            inventory.setCarriedItem(packet.getStack(), -1, Issuer.SERVER, event.getTimestamp());
        } else if (packetType == PacketType.Play.Server.OPEN_WINDOW) {
            //data.setOpenInventory(true);
        } else if (packetType == PacketType.Play.Server.OPEN_HORSE_WINDOW) {
            //data.setOpenInventory(true);
        } else if (packetType == PacketType.Play.Server.CLOSE_WINDOW) {
            data.setOpenInventory(false);
        }
    }

}
