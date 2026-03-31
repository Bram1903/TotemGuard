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

package com.deathmotion.totemguard.common.check.impl.tick;

import com.deathmotion.totemguard.api3.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

@RequiresTickEnd
@CheckData(description = "Forcefully prevents tick end packets", type = CheckType.TICK)
public class TickA extends CheckImpl implements PacketCheck {

    private boolean hasSentTickEnd = true;

    public TickA(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon packetType = event.getPacketType();

        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            if (!hasSentTickEnd) {
                if (buffer.increase(1) > 1) {
                    fail();
                }
            } else {
                buffer.decrease(1);
            }

            hasSentTickEnd = false;
        } else if (packetType == PacketType.Play.Client.CLIENT_TICK_END) {
            hasSentTickEnd = true;
        }
    }
}

