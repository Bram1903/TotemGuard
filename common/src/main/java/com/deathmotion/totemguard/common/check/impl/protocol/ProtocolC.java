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

package com.deathmotion.totemguard.common.check.impl.protocol;

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;

@RequiresTickEnd
@CheckData(description = "Invalid set slot", type = CheckType.PROTOCOL)
public class ProtocolC extends CheckImpl implements PacketCheck {

    private int currentSlot = -1;

    private int slotChanges;
    private int blockPlacements;

    public ProtocolC(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == PacketType.Play.Client.HELD_ITEM_CHANGE) {
            int slot = new WrapperPlayClientHeldItemChange(event).getSlot();
            
            if (currentSlot == -1) {
                currentSlot = slot;
                return;
            }

            if (slot != currentSlot) {
                slotChanges++;
            }

            currentSlot = slot;
        } else if (packetType == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
            blockPlacements++;

            if (blockPlacements > 1 && slotChanges > 1) {
                fail();
            }
        } else if (player.isTickEndPacket(packetType)) {
            slotChanges = 0;
            blockPlacements = 0;
        }
    }
}

