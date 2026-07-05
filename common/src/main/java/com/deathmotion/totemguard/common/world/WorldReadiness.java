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

package com.deathmotion.totemguard.common.world;

import com.deathmotion.totemguard.common.player.latency.PacketLatencyHandler;

// Judging against a half-streamed world is the join-time setback-storm bug.
public final class WorldReadiness {

    private static final int MAX_LOAD_TICKS = 100;

    private boolean ready;
    private int epoch;
    private int firedEpoch = -1;
    private int coastTicks;

    public boolean ready() {
        return ready;
    }

    public void reset() {
        ready = false;
        epoch = 0;
        firedEpoch = -1;
        coastTicks = 0;
    }

    public void onChunkApplied() {
        if (!ready) epoch++;
    }

    public void requestReadiness(PacketLatencyHandler latency) {
        if (ready) return;
        if (++coastTicks >= MAX_LOAD_TICKS) {
            ready = true;
            return;
        }
        if (firedEpoch == epoch) return;
        final int pendingEpoch = epoch;
        firedEpoch = pendingEpoch;
        latency.sendTransaction(timestamp -> {
            if (!ready && epoch == pendingEpoch) ready = true;
        });
    }
}
