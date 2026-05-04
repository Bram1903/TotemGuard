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

/**
 * Aggregated counts for a {@link StatsWindow}, taken across every player. Returned by
 * {@link StatsRepository#snapshot(StatsWindow)} inside a
 * {@link com.deathmotion.totemguard.api.result.Result}.
 *
 * <p>{@code databaseBytes} is the exact on-disk size for {@link StatsWindow#ALL_TIME}
 * (sum of {@code data_length + index_length} across the {@code tg_*} tables). For a
 * rolling window it is a proportional estimate of just the alert + punishment row
 * footprint (avg row size of those tables x rows in window), so it scales linearly
 * with the window length and is comparable across the 24h / 7d / 30d tabs.
 *
 * @param alertCount      total alerts logged in the window.
 * @param punishmentCount total punishments dispatched in the window.
 * @param uniquePlayers   distinct players who joined inside the window
 *                        ({@code COUNT(*) FROM tg_players WHERE last_seen >= since}),
 *                        or the row count of {@code tg_players} for all-time.
 * @param flaggedPlayers  distinct players who had at least one alert in the window
 *                        (driven by {@code tg_players.last_flagged_at}).
 * @param databaseBytes   storage attributable to the window (see class docs).
 */
public record StatsSnapshot(
        int alertCount,
        int punishmentCount,
        int uniquePlayers,
        int flaggedPlayers,
        long databaseBytes
) {
}
