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

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckData;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;

// Copied this check from GrimAC (Inventory related, and I don't want our packet based inventory to get out of sync)
@CheckData(description = "Invalid button for window click type", type = CheckType.PROTOCOL)
public class ProtocolB extends CheckImpl implements PacketCheck {

    public ProtocolB(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLICK_WINDOW) return;

        WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);
        WrapperPlayClientClickWindow.WindowClickType clickType = packet.getWindowClickType();
        int button = packet.getButton();

        boolean wrong = switch (clickType) {
            case PICKUP, QUICK_MOVE, CLONE -> button > 2 || button < 0;
            case SWAP -> (button > 8 || button < 0) && button != 40;
            case THROW -> button != 0 && button != 1;
            case QUICK_CRAFT -> button == 3 || button == 7 || button > 10 || button < 0;
            case PICKUP_ALL -> button != 0;
            case UNKNOWN -> false;
        };

        if (wrong) {
            fail("button: " + button + ", click type: " + clickType);
        }
    }
}

