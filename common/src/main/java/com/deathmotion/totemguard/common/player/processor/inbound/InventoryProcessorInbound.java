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

package com.deathmotion.totemguard.common.player.processor.inbound;

import com.deathmotion.totemguard.common.player.inventory.PacketInventory;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.*;

import java.util.Map;
import java.util.Optional;

public class InventoryProcessorInbound extends ProcessorInbound {

    private final PacketInventory packetInventory;

    public InventoryProcessorInbound(TGPlayer player) {
        super(player);
        this.packetInventory = player.getPacketInventory();
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && !event.isCancelled()) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
        }
        else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING && !event.isCancelled()) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = packet.getAction();

            switch (action) {
                case DROP_ITEM_STACK -> {
                    packetInventory.removeItemFromHand();
                }
                case DROP_ITEM -> {
                    packetInventory.removeItemFromHand(1);
                }
                case SWAP_ITEM_WITH_OFFHAND -> {
                    packetInventory.swapItemToOffhand();
                }
            }
        }
        else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE && !event.isCancelled()) {
            WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);
            int slot = packet.getSlot();

            if (slot > 8 || slot < 0) return;
            packetInventory.setSelectedSlot(slot);
        }
        else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION && !event.isCancelled()) {
            WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);

            // TODO: We need to keep track of the player's gamemode properly for this to work
            //if (player.gamemode != GameMode.CREATIVE) return;

            boolean valid = packet.getSlot() >= 1 && (PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_8) ? packet.getSlot() <= 45 : packet.getSlot() < 45);

            if (!valid) return;
            packetInventory.setItem(packet.getSlot(), packet.getItemStack());
        }
        else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW && !event.isCancelled()) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
            if (packet.getWindowId() != PacketInventory.PLAYER_WINDOW_ID) return;

            packet.getSlots().ifPresent(slots -> {
                for (Map.Entry<Integer, ItemStack> slotEntry : slots.entrySet()) {
                    packetInventory.setItem(slotEntry.getKey(), slotEntry.getValue());
                }
            });

            packetInventory.setCarriedItem(packet.getCarriedItemStack());
        }
    }
}
