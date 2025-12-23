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
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

public class InventoryProcessorInbound extends ProcessorInbound {

    private final PacketInventory packetInventory;

    public InventoryProcessorInbound(TGPlayer player) {
        super(player);
        this.packetInventory = player.getPacketInventory();
    }

    @Override
    public void handleInboundPost(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM && !event.isCancelled()) {

        }
        else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING && !event.isCancelled()) {

        }
        else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE && !event.isCancelled()) {

        }
        else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION && !event.isCancelled()) {

        }
        else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW && !event.isCancelled()) {

        }
    }
}
