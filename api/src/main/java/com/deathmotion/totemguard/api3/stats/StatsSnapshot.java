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

package com.deathmotion.totemguard.api3.stats;

/**
 * Aggregated counts for a {@link StatsWindow}, taken across every player. Returned by
 * {@link StatsRepository#snapshot(StatsWindow)} inside a
 * {@link com.deathmotion.totemguard.api3.result.Result}.
 *
 * <p>{@code databaseBytes} is exact for {@link StatsWindow#ALL_TIME} (sum of
 * {@code data_length + index_length} across the {@code tg_*} tables) and a per-window
 * estimate otherwise (avg row size of the event tables × rows in window). Treat the
 * windowed value as a magnitude indicator, not a precise figure.
 *
 * @param alertCount      total alerts logged in the window
 * @param punishmentCount total punishments dispatched in the window
 * @param uniquePlayers   distinct players with {@code last_seen} inside the window
 *                        ({@code COUNT(*) FROM tg_players} for all-time)
 * @param databaseBytes   storage attributable to the window (see class docs)
 */
public record StatsSnapshot(
        int alertCount,
        int punishmentCount,
        int uniquePlayers,
        long databaseBytes
) {
}
