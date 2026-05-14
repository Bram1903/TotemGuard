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

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.ping.PingData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(description = "Invalid acknowledgement order", type = CheckType.TICK)
public class TickB extends CheckImpl implements PacketCheck {

    public TickB(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        PingData pingData = player.getPingData();

        if (packetType == PacketType.Play.Client.WINDOW_CONFIRMATION || packetType == PacketType.Play.Client.PONG) {
            if (!pingData.isLastTransactionReplyValid()) {
                fail("type=transaction,invalid");
                return;
            }

            if (pingData.isLastTransactionReplySkipped()) {
                fail("type=transaction,skipped={0}", pingData.getLastSkippedTransactionReplyCount());
            }
        } else if (packetType == PacketType.Play.Client.KEEP_ALIVE) {
            if (platform.getConfigRepository().configView().tickSkipKeepAliveValidation()) {
                return;
            }

            if (!pingData.isLastKeepAliveReplyValid()) {
                fail("type=keepalive,invalid");
                return;
            }

            if (pingData.isLastKeepAliveReplySkipped()) {
                fail("type=keepalive,skipped={0}", pingData.getLastSkippedKeepAliveReplyCount());
            }
        } else if (packetType == PacketType.Play.Client.TELEPORT_CONFIRM) {
            if (pingData.isLastTeleportReplySkipped()) {
                fail("type=teleport,skipped={0}", pingData.getLastSkippedTeleportReplyCount());
            }
        }
    }
}
