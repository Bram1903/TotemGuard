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
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.annotations.RequiresTickEnd;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;

@RequiresTickEnd
@CheckData(description = "Duplicate consecutive player input", type = CheckType.PROTOCOL)
public class ProtocolG extends CheckImpl implements PacketCheck {

    private boolean hasPrevious;
    private byte previousMask;

    public ProtocolG(TGPlayer player) {
        super(player);
    }

    private static byte encode(WrapperPlayClientPlayerInput packet) {
        int v = 0;
        if (packet.isForward()) v |= 1;
        if (packet.isBackward()) v |= 1 << 1;
        if (packet.isLeft()) v |= 1 << 2;
        if (packet.isRight()) v |= 1 << 3;
        if (packet.isJump()) v |= 1 << 4;
        if (packet.isShift()) v |= 1 << 5;
        if (packet.isSprint()) v |= 1 << 6;
        return (byte) v;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLAYER_INPUT) return;

        WrapperPlayClientPlayerInput packet = new WrapperPlayClientPlayerInput(event);
        byte mask = encode(packet);

        if (hasPrevious && mask == previousMask) {
            fail("mask=" + Integer.toBinaryString(mask & 0xFF));
        }

        hasPrevious = true;
        previousMask = mask;
    }
}
