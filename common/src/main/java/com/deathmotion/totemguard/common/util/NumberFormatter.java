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

package com.deathmotion.totemguard.common.util;

import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class NumberFormatter {

    /**
     * Formats a count with a thousand separators, e.g. {@code 11323049 -> "11,323,049"}.
     */
    public String grouped(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * Formats a count in compact form for tight UI slots, e.g. {@code 11323049 -> "11.3M"}.
     * Falls back to {@link #grouped(long)} for values below 10,000.
     */
    public String compact(long value) {
        long abs = Math.abs(value);
        if (abs < 10_000L) return grouped(value);
        if (abs < 1_000_000L) return String.format(Locale.ROOT, "%.1fK", value / 1_000.0);
        if (abs < 1_000_000_000L) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        if (abs < 1_000_000_000_000L) return String.format(Locale.ROOT, "%.1fB", value / 1_000_000_000.0);
        return String.format(Locale.ROOT, "%.1fT", value / 1_000_000_000_000.0);
    }
}
