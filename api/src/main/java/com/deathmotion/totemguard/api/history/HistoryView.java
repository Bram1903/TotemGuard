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

package com.deathmotion.totemguard.api.history;

import com.deathmotion.totemguard.api.result.Result;
import com.deathmotion.totemguard.api.stats.StatsWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * History queries scoped to one player UUID. All methods are paginated. Every method
 * completes asynchronously with a {@link Result} (never exceptionally for expected
 * failures like database offline). Window overloads restrict to events whose
 * {@code created_at} falls in the window, no-window overloads behave like
 * {@link StatsWindow#ALL_TIME}. {@code checkName} is exact and case-sensitive,
 * {@code null} returns all checks. Negative {@code page} is treated as {@code 0}, past
 * the end yields an empty page.
 */
public interface HistoryView {

    /**
     * UUID this view is bound to, the same value passed when it was created.
     */
    @NotNull UUID uuid();

    /**
     * Entries-per-page cap for every query on this view. Forwarded from
     * {@link HistoryRepository#pageSize()} so all views in the same install agree.
     */
    int pageSize();

    /**
     * One page of alerts, newest-first, across all checks and all time.
     */
    @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page);

    /**
     * One page of alerts filtered to {@code checkName}, or all checks when {@code null}.
     */
    @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page, @Nullable String checkName);

    /**
     * One page of alerts restricted to the given time window.
     */
    @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page, @NotNull StatsWindow window);

    /**
     * One page of alerts filtered by both check name and time window.
     */
    @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page, @Nullable String checkName, @NotNull StatsWindow window);

    /**
     * Total alerts on record across all checks and all time.
     */
    @NotNull CompletableFuture<Result<Integer>> alertCount();

    /**
     * Total alerts for {@code checkName}, or all checks when {@code null}.
     */
    @NotNull CompletableFuture<Result<Integer>> alertCount(@Nullable String checkName);

    /**
     * Total alerts in the given time window across all checks.
     */
    @NotNull CompletableFuture<Result<Integer>> alertCount(@NotNull StatsWindow window);

    /**
     * Total alerts filtered by both check name and time window.
     */
    @NotNull CompletableFuture<Result<Integer>> alertCount(@Nullable String checkName, @NotNull StatsWindow window);

    /**
     * One page of punishments, newest-first, across all time.
     */
    @NotNull CompletableFuture<Result<HistoryPage<PunishmentEntry>>> punishments(int page);

    /**
     * One page of punishments restricted to the given time window.
     */
    @NotNull CompletableFuture<Result<HistoryPage<PunishmentEntry>>> punishments(int page, @NotNull StatsWindow window);

    /**
     * Total punishments on record across all time.
     */
    @NotNull CompletableFuture<Result<Integer>> punishmentCount();

    /**
     * Total punishments in the given time window.
     */
    @NotNull CompletableFuture<Result<Integer>> punishmentCount(@NotNull StatsWindow window);
}
