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

package com.deathmotion.totemguard.api.stats;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * A time window over which {@link StatsRepository} aggregates alert and punishment counts.
 * Use {@link #ALL_TIME} for an unbounded total, or one of the rolling windows to limit
 * the count to events whose {@code created_at} falls within {@link #window()} of "now".
 */
public enum StatsWindow {

    ALL_TIME("all", "All time", null),
    LAST_30_DAYS("30d", "Last 30 days", Duration.ofDays(30)),
    LAST_7_DAYS("7d", "Last 7 days", Duration.ofDays(7)),
    LAST_24_HOURS("24h", "Last 24 hours", Duration.ofHours(24));

    private final String id;
    private final String label;
    private final @Nullable Duration window;

    StatsWindow(String id, String label, @Nullable Duration window) {
        this.id = id;
        this.label = label;
        this.window = window;
    }

    /**
     * Stable identifier for this window, suitable for cache keys and configuration values.
     */
    public String id() {
        return id;
    }

    /**
     * Human-readable label for display in UIs.
     */
    public String label() {
        return label;
    }

    /**
     * The rolling duration this window covers, or {@code null} for {@link #ALL_TIME}.
     */
    public @Nullable Duration window() {
        return window;
    }

    /**
     * Whether this window is unbounded.
     */
    public boolean isAllTime() {
        return window == null;
    }
}
