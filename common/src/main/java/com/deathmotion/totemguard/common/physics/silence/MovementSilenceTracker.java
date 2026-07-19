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

package com.deathmotion.totemguard.common.physics.silence;

public final class MovementSilenceTracker {

    private static final int POSITION_REMINDER_TICKS = 20;
    private static final int STARVED_TICKS = POSITION_REMINDER_TICKS + 5;
    private static final int QUIET_TICK_CAP = 10_000;
    private static final long PROBE_INTERVAL_NANOS = 10_000_000_000L;

    private int quietTicks;
    private long lastPositionNanos;
    private long lastProbeNanos;

    public void onPositionPacket(long nowNanos) {
        quietTicks = 0;
        lastPositionNanos = nowNanos;
        lastProbeNanos = 0L;
    }

    public boolean clockTick() {
        if (quietTicks < QUIET_TICK_CAP) quietTicks++;
        return quietTicks >= STARVED_TICKS;
    }

    public boolean probeWanted(long nowNanos) {
        if (lastPositionNanos == 0L) return false;
        if (nowNanos - lastPositionNanos < PROBE_INTERVAL_NANOS) return false;
        if (lastProbeNanos != 0L && nowNanos - lastProbeNanos < PROBE_INTERVAL_NANOS) return false;
        lastProbeNanos = nowNanos;
        return true;
    }

    public void reset(long nowNanos) {
        quietTicks = 0;
        lastPositionNanos = nowNanos;
        lastProbeNanos = 0L;
    }
}
