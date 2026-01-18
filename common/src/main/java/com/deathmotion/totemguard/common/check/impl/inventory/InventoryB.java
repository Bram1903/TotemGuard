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
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

@CheckData(description = "Impossible click time difference", type = CheckType.INVENTORY)
public class InventoryB extends CheckImpl implements PacketCheck {

    private static final int LEFT_CLICK = 0;
    private static final int RIGHT_CLICK = 1;

    private long lastLeftClick = -1;
    private long lastRightClick = -1;

    public InventoryB(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        if (packet.getWindowClickType() != WrapperPlayClientClickWindow.WindowClickType.PICKUP) return;

        int button = packet.getButton();
        if (button != LEFT_CLICK && button != RIGHT_CLICK) return;

        long lastClickTime = button == LEFT_CLICK ? lastLeftClick : lastRightClick;

        if (lastClickTime != -1) {
            long clickDifference = event.getTimestamp() - lastClickTime;

            if (clickDifference < 5) {
                fail("diff: " + clickDifference);
            }
        }

        if (button == LEFT_CLICK) {
            lastLeftClick = event.getTimestamp();
        } else {
            lastRightClick = event.getTimestamp();
        }
    }

}