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

package com.deathmotion.totemguard.common.replay.playback;

import com.deathmotion.totemguard.common.util.SessionClock;

public final class ReplayClock implements SessionClock {

    private final long startNanos;
    private final long startEpochMillis;

    private long nanos;

    public ReplayClock(long startNanos, long startEpochMillis) {
        this.startNanos = startNanos;
        this.startEpochMillis = startEpochMillis;
        this.nanos = startNanos;
    }

    public void advanceTo(long frameNanos) {
        if (frameNanos > nanos) nanos = frameNanos;
    }

    @Override
    public long nanos() {
        return nanos;
    }

    @Override
    public long millis() {
        return startEpochMillis + (nanos - startNanos) / 1_000_000L;
    }
}
