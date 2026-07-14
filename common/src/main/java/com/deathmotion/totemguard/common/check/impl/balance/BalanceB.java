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

import com.deathmotion.totemguard.api.check.CheckType;
import com.deathmotion.totemguard.common.check.CheckImpl;
import com.deathmotion.totemguard.common.check.annotations.CheckData;
import com.deathmotion.totemguard.common.check.type.PacketCheck;
import com.deathmotion.totemguard.common.player.TGPlayer;
import com.deathmotion.totemguard.common.player.data.MovementData;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

@CheckData(description = "Game clock running slower than real time", type = CheckType.TICK,
        experimental = true)
public class BalanceB extends CheckImpl implements PacketCheck {

    private static final long NEGATIVE_DRIFT_NANOS = 1_200_000_000L;
    private static final long STREAM_GAP_NANOS = 2L * GameClock.TICK_NANOS;
    private static final long JOIN_GRACE_NANOS = 60_000_000_000L;
    private static final double SKIP_THRESHOLD = 0.03;
    private static final double BUFFER_GAIN = 1.0;
    private static final double BUFFER_DECAY = 0.0025;
    private static final double BUFFER_THRESHOLD = 2.0;
    private static final double BUFFER_RETAIN = 1.0;

    private final GameClock clock;
    private long previousReferenceNanos;

    public BalanceB(TGPlayer player) {
        super(player);
        this.clock = new GameClock(player, System.nanoTime() - JOIN_GRACE_NANOS);
        this.previousReferenceNanos = clock.referenceNanos();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!clock.onClientTick(event.getPacketType())) return;

        long reference = clock.referenceNanos();
        long referenceGap = reference - previousReferenceNanos;
        previousReferenceNanos = reference;

        long now = System.nanoTime();
        if (data.isDead() || referenceGap > STREAM_GAP_NANOS || !tickingReliably()) {
            clock.raiseTo(Math.min(now, reference));
            buffer.decrease(BUFFER_DECAY);
            return;
        }

        clock.advance();
        clock.lowerTo(now);

        long behind = reference - NEGATIVE_DRIFT_NANOS - clock.virtualNanos();
        if (behind <= 0) {
            buffer.decrease(BUFFER_DECAY);
            return;
        }

        clock.advance();
        if (buffer.increase(BUFFER_GAIN) >= BUFFER_THRESHOLD) {
            buffer.set(BUFFER_RETAIN);
            fail("behind={0}ms,ping={1}ms", behind / 1_000_000L,
                    player.getPingData().getTransactionPing());
        }
    }

    private boolean tickingReliably() {
        if (player.supportsEndTick()) return true;
        if (data.isInVehicle()) return false;
        MovementData movement = data.getMovementData();
        if (!movement.isLastFlyingPositionChanged()) return false;
        double dx = movement.getCurrent().getX() - movement.getPrevious().getX();
        double dy = movement.getCurrent().getY() - movement.getPrevious().getY();
        double dz = movement.getCurrent().getZ() - movement.getPrevious().getZ();
        return Math.abs(dx) > SKIP_THRESHOLD || Math.abs(dy) > SKIP_THRESHOLD
                || Math.abs(dz) > SKIP_THRESHOLD;
    }
}
