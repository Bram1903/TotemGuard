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

import com.deathmotion.totemguard.api.result.Result;
import com.deathmotion.totemguard.api.result.ResultError;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Aggregate alert and punishment statistics. Reads consult the cache chain (memory,
 * Redis, database) and are cheap within the TTL. Counts are global across every player
 * on every server sharing the database. Offline calls settle with
 * {@link ResultError#DATABASE_UNAVAILABLE} rather than completing exceptionally.
 */
public interface StatsRepository {

    /**
     * Aggregate counts for the given window. The future never completes exceptionally
     * for expected failures (database offline, query error), inspect the {@link Result}.
     */
    @NotNull CompletableFuture<Result<StatsSnapshot>> snapshot(@NotNull StatsWindow window);
}
