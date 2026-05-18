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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * A time window over which {@link StatsRepository} aggregates counts. Rolling windows
 * limit events to those whose {@code created_at} falls within {@link #window()} of now.
 */
public enum StatsWindow {

    /**
     * Unbounded, every row TotemGuard has on disk. {@link #window()} returns {@code null}.
     */
    ALL_TIME("all", "All time", null),
    /**
     * Rolling 30-day window, the longest bounded period exposed.
     */
    LAST_30_DAYS("30d", "Last 30 days", Duration.ofDays(30)),
    /**
     * Rolling 7-day window, the weekly view.
     */
    LAST_7_DAYS("7d", "Last 7 days", Duration.ofDays(7)),
    /**
     * Rolling 24-hour window, the daily view.
     */
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
     * Short stable identifier ({@code "all"}, {@code "30d"}, {@code "7d"}, {@code "24h"}),
     * safe to use as a cache key segment or YAML value.
     */
    public @NotNull String id() {
        return id;
    }

    /**
     * Human-readable label rendered in stats GUIs (e.g. {@code "Last 24 hours"}).
     */
    public @NotNull String label() {
        return label;
    }

    /**
     * Rolling duration applied against {@code created_at}, or {@code null} for
     * {@link #ALL_TIME}. Same value the SQL window clause is built from.
     */
    public @Nullable Duration window() {
        return window;
    }

    /**
     * Whether this window is {@link #ALL_TIME} (i.e. {@link #window()} is {@code null}).
     */
    public boolean isAllTime() {
        return window == null;
    }
}
