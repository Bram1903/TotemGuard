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

import com.deathmotion.totemguard.api3.result.Result;
import com.deathmotion.totemguard.api3.result.ResultError;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Entry point for aggregate alert and punishment statistics. Reads always consult
 * TotemGuard's cache (local memory, then Redis, then the database) so repeated calls
 * within the cache TTL are cheap.
 * <p>
 * Snapshots are global: counts cover every player on every server sharing the database.
 * Statistics are only available while the database is reachable; offline calls settle
 * with {@link ResultError#DATABASE_UNAVAILABLE} rather than completing exceptionally.
 */
public interface StatsRepository {

    /**
     * Returns aggregate counts for the given window.
     * <p>
     * The future settles with a {@link Result} carrying either the counts or a failure
     * reason; it does not complete exceptionally for the database being offline or a
     * query failing.
     *
     * @param window the window to aggregate over, must not be {@code null}
     */
    @NotNull CompletableFuture<Result<StatsSnapshot>> snapshot(@NotNull StatsWindow window);
}
