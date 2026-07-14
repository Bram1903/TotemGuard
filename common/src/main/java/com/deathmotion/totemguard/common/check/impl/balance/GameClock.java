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

package com.deathmotion.totemguard.common.check.impl.balance;

import com.deathmotion.totemguard.common.player.TGPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;

final class GameClock {

    static final long TICK_NANOS = 50_000_000L;

    private final TGPlayer player;
    private long referenceNanos;
    private long virtualNanos;
    private boolean flyingSampledThisTick;

    GameClock(TGPlayer player, long startNanos) {
        this.player = player;
        this.referenceNanos = startNanos;
        this.virtualNanos = startNanos;
    }

    boolean onClientTick(PacketTypeCommon packetType) {
        if (!countsAsClientTick(packetType)) return false;
        long gated = player.getPingData().getGatedTransactionAnchorNanos();
        if (gated != 0L) referenceNanos = gated;
        player.getPingData().markMovementForTransactionAnchor();
        return true;
    }

    long referenceNanos() {
        return referenceNanos;
    }

    long virtualNanos() {
        return virtualNanos;
    }

    void advance() {
        virtualNanos += TICK_NANOS;
    }

    void rewind() {
        virtualNanos -= TICK_NANOS;
    }

    void raiseTo(long floor) {
        if (virtualNanos < floor) virtualNanos = floor;
    }

    void lowerTo(long ceiling) {
        if (virtualNanos > ceiling) virtualNanos = ceiling;
    }

    private boolean countsAsClientTick(PacketTypeCommon packetType) {
        if (WrapperPlayClientPlayerFlying.isFlying(packetType)) {
            boolean counted = !player.getData().getTeleportData().lastPacketWasTeleport()
                    && !player.getData().getMovementData().isLastFlyingWasDuplicate();
            flyingSampledThisTick = true;
            return counted;
        }

        if (packetType == PacketType.Play.Client.CLIENT_TICK_END && player.supportsEndTick()) {
            boolean hadFlying = flyingSampledThisTick;
            flyingSampledThisTick = false;
            return !hadFlying;
        }

        return false;
    }
}
