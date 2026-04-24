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

package com.deathmotion.totemguard.common.gui.screen.history;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class HistoryText {

    private static final DateTimeFormatter ABSOLUTE = DateTimeFormatter
            .ofPattern("dd-MM-yyyy HH:mm", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private HistoryText() {
    }

    static String relative(long epochMs) {
        long deltaSeconds = Math.max(0, (System.currentTimeMillis() - epochMs) / 1000);
        if (deltaSeconds < 60) return deltaSeconds + "s ago";
        if (deltaSeconds < 3600) return (deltaSeconds / 60) + "m ago";
        if (deltaSeconds < 86_400) return (deltaSeconds / 3600) + "h ago";
        if (deltaSeconds < 2_592_000) return (deltaSeconds / 86_400) + "d ago";
        return (deltaSeconds / 2_592_000) + "mo ago";
    }

    static String absolute(long epochMs) {
        return ABSOLUTE.format(Instant.ofEpochMilli(epochMs));
    }
}
