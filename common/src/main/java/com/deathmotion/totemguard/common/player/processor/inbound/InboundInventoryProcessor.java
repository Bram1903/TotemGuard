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

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.*;
import com.deathmotion.totemguard.common.player.processor.ProcessorInbound;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.*;

import java.util.Map;

public class InboundInventoryProcessor extends ProcessorInbound {

    private final PacketInventory inventory;

    public InboundInventoryProcessor(TGPlayer player) {
        super(player);
        this.inventory = player.getInventory();
    }

    @Override
    public void handleInbound(PacketReceiveEvent event) {
        if (event.isCancelled()) return;
        final PacketTypeCommon packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);

            int usedItemSlot = packet.getHand() == InteractionHand.MAIN_HAND
                    ? inventory.getMainHandSlot()
                    : InventoryConstants.SLOT_OFFHAND;

            ItemStack usedItem = inventory.getItem(usedItemSlot);

            EquipmentType equipmentType = EquipmentType.getEquipmentSlotForItem(usedItem);
            if (equipmentType == null) return;

            int slot;
            switch (equipmentType) {
                case HEAD -> slot = InventoryConstants.SLOT_HELMET;
                case CHEST -> slot = InventoryConstants.SLOT_CHESTPLATE;
                case LEGS -> slot = InventoryConstants.SLOT_LEGGINGS;
                case FEET -> slot = InventoryConstants.SLOT_BOOTS;
                default -> {
                    return;
                }
            }

            ItemStack currentEquippedItem = inventory.getItem(slot);
            // Only 1.19.4+ clients support swapping with non-empty items
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_19_4) && !currentEquippedItem.isEmpty()) return;

            // TODO: When the armor stack has an amount greater than one, we need to properly handle it

            inventory.setItem(usedItemSlot, currentEquippedItem, ChangeOrigin.CLIENT, SetSlotAction.SWAP, event.getTimestamp());
            inventory.setItem(slot, usedItem, ChangeOrigin.CLIENT, SetSlotAction.SWAP, event.getTimestamp());
        } else if (packetType == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            DiggingAction action = packet.getAction();

            switch (action) {
                case DROP_ITEM_STACK -> {
                    inventory.removeItemFromHand();
                }
                case DROP_ITEM -> {
                    inventory.removeItemFromHand(1);
                }
                case SWAP_ITEM_WITH_OFFHAND -> {
                    inventory.swapItemToOffhand(ChangeOrigin.CLIENT, event.getTimestamp());
                }
            }
        } else if (packetType == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);
            int slot = packet.getSlot();

            if (slot > 8 || slot < 0) return;
            inventory.setSelectedHotbarIndex(slot);
        } else if (packetType == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);

            // TODO: We need to keep track of the player's gamemode properly for this to work
            //if (player.gamemode != GameMode.CREATIVE) return;

            boolean valid = packet.getSlot() >= 1 && (PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_8) ? packet.getSlot() <= 45 : packet.getSlot() < 45);

            if (!valid) return;
            inventory.setItem(packet.getSlot(), packet.getItemStack(), ChangeOrigin.CLIENT, SetSlotAction.CLICK, event.getTimestamp());
        } else if (packetType == PacketType.Play.Client.CLICK_WINDOW) {
            WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
            if (packet.getWindowId() != InventoryConstants.PLAYER_WINDOW_ID) return;

            inventory.setCarriedItem(packet.getCarriedItemStack());
            packet.getSlots().ifPresent(slots -> {
                for (Map.Entry<Integer, ItemStack> slotEntry : slots.entrySet()) {
                    inventory.setItem(slotEntry.getKey(), slotEntry.getValue(), ChangeOrigin.CLIENT, SetSlotAction.CLICK, event.getTimestamp());
                }
            });
        }
    }
}
