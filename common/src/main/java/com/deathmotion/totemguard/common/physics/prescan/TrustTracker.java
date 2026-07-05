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

package com.deathmotion.totemguard.common.physics.prescan;

public final class TrustTracker {

    public enum Trust {
        TRUSTED,
        TRUSTED_ZERO,
        WITHHELD,
        COAST_DOUBLE,
        JUDGED_DOUBLE
    }

    private int flyingSinceTickEnd;
    private int doubleMoveStreak;

    public void countFlying(boolean teleportResync) {
        if (!teleportResync && flyingSinceTickEnd < 1000) flyingSinceTickEnd++;
    }

    public Trust classify(boolean positionChanged, boolean duplicate, boolean supportsEndTick,
                            boolean lastPacketWasTeleport, int doubleMoveGraceTicks) {
        if (!positionChanged) {
            if (duplicate) {
                doubleMoveStreak = 0;
                return Trust.TRUSTED_ZERO;
            }
            return Trust.WITHHELD;
        }
        boolean doubleMove = supportsEndTick && flyingSinceTickEnd > 1 && !lastPacketWasTeleport;
        if (!doubleMove) {
            doubleMoveStreak = 0;
            return Trust.TRUSTED;
        }
        return ++doubleMoveStreak <= doubleMoveGraceTicks ? Trust.COAST_DOUBLE : Trust.JUDGED_DOUBLE;
    }

    public void clearDoubleMoveStreak() {
        doubleMoveStreak = 0;
    }

    public boolean onTickEnd() {
        boolean sawFlying = flyingSinceTickEnd > 0;
        flyingSinceTickEnd = 0;
        return sawFlying;
    }

    public void reset() {
        flyingSinceTickEnd = 0;
        doubleMoveStreak = 0;
    }
}
