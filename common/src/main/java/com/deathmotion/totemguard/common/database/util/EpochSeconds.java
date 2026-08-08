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

package com.deathmotion.totemguard.common.database.util;

import java.util.concurrent.TimeUnit;

public final class EpochSeconds {

    private static final long MAX_INT_UNSIGNED = (1L << 32) - 1L;

    private EpochSeconds() {
    }

    public static int fromMillis(long epochMs) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(epochMs);
        if (seconds < 0L) return 0;
        if (seconds > MAX_INT_UNSIGNED) return (int) MAX_INT_UNSIGNED;
        return (int) seconds;
    }

    public static long toMillis(long epochSeconds) {
        return TimeUnit.SECONDS.toMillis(epochSeconds & MAX_INT_UNSIGNED);
    }

    public static int dayFromMillis(long epochMs) {
        return (int) TimeUnit.MILLISECONDS.toDays(Math.max(0L, epochMs));
    }

    public static int dayFromSeconds(int epochSeconds) {
        long unsigned = epochSeconds & MAX_INT_UNSIGNED;
        return (int) TimeUnit.SECONDS.toDays(unsigned);
    }
}
