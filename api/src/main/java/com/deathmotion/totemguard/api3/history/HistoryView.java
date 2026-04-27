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

package com.deathmotion.totemguard.api3.history;

import com.deathmotion.totemguard.api3.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * History queries scoped to a single player UUID. All methods are paginated —
 * there is no bulk "fetch everything" path, so a single API call can never return
 * more than {@link HistoryRepository#pageSize()} rows. Reads consult the in-memory
 * and Redis caches before falling through to the database.
 * <p>
 * Every method completes asynchronously on TotemGuard's worker pool with a
 * {@link Result} — the future itself never completes exceptionally for
 * expected failures (database offline, query error). Inspect {@link Result#ok()}
 * to branch.
 */
public interface HistoryView {

    /**
     * The UUID this view targets — the same one passed to {@link HistoryRepository#of(UUID)}.
     */
    @NotNull UUID uuid();

    /**
     * Convenience accessor; identical to {@link HistoryRepository#pageSize()}.
     */
    int pageSize();

    /**
     * Fetches one page of alerts (newest-first) for this user.
     *
     * @param page zero-based page index. Negative values are treated as {@code 0};
     *             values past the end yield an empty page.
     */
    @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page);

    /**
     * Same as {@link #alerts(int)} but restricted to a specific check name.
     *
     * @param checkName exact check name (e.g. {@code AutoTotemA}) — case-sensitive,
     *                  matched against the stored value. {@code null} returns all checks.
     */
    @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page, @Nullable String checkName);

    /**
     * Total number of alerts on record for this user (no filter).
     */
    @NotNull CompletableFuture<Result<Integer>> alertCount();

    /**
     * Total alerts matching {@code checkName}, or all alerts if {@code null}.
     */
    @NotNull CompletableFuture<Result<Integer>> alertCount(@Nullable String checkName);

    /**
     * Fetches one page of punishments (newest-first) for this user.
     */
    @NotNull CompletableFuture<Result<HistoryPage<PunishmentEntry>>> punishments(int page);

    /**
     * Total number of punishments on record for this user.
     */
    @NotNull CompletableFuture<Result<Integer>> punishmentCount();
}
