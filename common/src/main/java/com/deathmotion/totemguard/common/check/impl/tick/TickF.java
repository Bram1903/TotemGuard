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
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;

@CheckData(description = "Answering transactions while withholding position updates", type = CheckType.TICK,
        experimental = true)
public class TickF extends CheckImpl implements PacketCheck {

    private static final long WINDOW_NANOS = 2_000_000_000L;
    private static final double BUFFER_GAIN = 1.0;
    private static final double BUFFER_DECAY = 0.25;
    private static final double BUFFER_THRESHOLD = 2.0;
    private static final double BUFFER_RETAIN = 1.0;

    private boolean windowStarted;
    private long windowWallNanos;
    private long windowClockNanos;
    private boolean positionSeen;

    public TickF(TGPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();

        if (carriesPosition(packetType)) {
            positionSeen = true;
            return;
        }

        if (packetType != PacketType.Play.Client.PONG) return;
        if (!player.getPingData().isLastTransactionReplyValid()) return;

        long now = System.nanoTime();
        long clock = player.getPingData().getLastAckedTransactionSentNanos();
        if (!windowStarted) {
            if (clock == 0L) return;
            startWindow(now, clock);
            return;
        }
        if (now - windowWallNanos <= WINDOW_NANOS || clock - windowClockNanos <= WINDOW_NANOS) return;

        boolean starved = !positionSeen
                && data.getMovementData().isCameraIsSelf()
                && !data.isDead();
        if (starved) {
            if (buffer.increase(BUFFER_GAIN) >= BUFFER_THRESHOLD) {
                buffer.set(BUFFER_RETAIN);
                fail("starved={0}ms", (now - windowWallNanos) / 1_000_000L);
            }
        } else {
            buffer.decrease(BUFFER_DECAY);
        }
        startWindow(now, clock);
    }

    private boolean carriesPosition(PacketTypeCommon packetType) {
        if (data.isInVehicle()) {
            return packetType == PacketType.Play.Client.PLAYER_ROTATION
                    || packetType == PacketType.Play.Client.STEER_VEHICLE
                    || packetType == PacketType.Play.Client.VEHICLE_MOVE
                    || packetType == PacketType.Play.Client.PLAYER_INPUT;
        }
        return packetType == PacketType.Play.Client.PLAYER_POSITION
                || packetType == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION;
    }

    private void startWindow(long now, long clock) {
        windowStarted = true;
        windowWallNanos = now;
        windowClockNanos = clock;
        positionSeen = false;
    }
}
