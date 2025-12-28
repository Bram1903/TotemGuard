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

package com.deathmotion.totemguard.common.player.processor.outbound;

import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.event.internal.impl.TotemActivatedEvent;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.deathmotion.totemguard.common.player.processor.ProcessorOutbound;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;

public class OutboundTotemActivatedProcessor extends ProcessorOutbound {

    public OutboundTotemActivatedProcessor(TGPlayer player) {
        super(player);
    }

    @Override
    public void handleOutbound(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.SET_SLOT) return;

        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);
        if (packet.getWindowId() != InventoryConstants.PLAYER_WINDOW_ID) return;

        int slot = packet.getSlot();
        ItemStack itemStack = packet.getItem();
        boolean isCarryingTotem = player.getInventory().isTotemInSlot(slot);

        if (isCarryingTotem && itemStack.isEmpty()) {
            player.setLastTotemUse(event.getTimestamp());
            TGPlatform.getInstance().getEventRepository().post(new TotemActivatedEvent(player, event.getTimestamp()));
        }
    }
}
