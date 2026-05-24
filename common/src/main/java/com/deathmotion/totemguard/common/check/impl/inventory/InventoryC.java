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

package com.deathmotion.totemguard.common.check.impl.inventory;

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.inventory.InventoryConstants;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;

@RequiresTickEnd
@CheckData(description = "Impossible inventory packet sequence", type = CheckType.INVENTORY)
public class InventoryC extends CheckImpl implements PacketCheck {

    private int closesInTick;

    public InventoryC(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();

        if (type == PacketType.Play.Client.CLIENT_TICK_END) {
            closesInTick = 0;
            return;
        }

        if (data.isInventoryMitigatedThisTick()) return;

        if (type == PacketType.Play.Client.CLICK_WINDOW) {
            int windowId = new WrapperPlayClientClickWindow(event).getWindowId();
            if (windowId != InventoryConstants.PLAYER_WINDOW_ID) return;
            if (closesInTick > 0) {
                fail("click after close (closes={0})", closesInTick);
            }
            return;
        }

        if (type == PacketType.Play.Client.CLOSE_WINDOW) {
            int windowId = new WrapperPlayClientCloseWindow(event).getWindowId();
            if (windowId != InventoryConstants.PLAYER_WINDOW_ID) return;
            closesInTick++;
            if (closesInTick >= 2) {
                fail("multiple closes (closes={0})", closesInTick);
            }
        }
    }
}
