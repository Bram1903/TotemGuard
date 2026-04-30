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
import com.deathmotion.totemguard.api.user.TGUser;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Entry point for paginated alert and punishment history. Reads always consult
 * TotemGuard's cache (local memory → Redis → database) so repeated lookups are
 * cheap; writes (i.e. {@link #clear}) invalidate every cached page for the target.
 * <p>
 * This API is intentionally narrow: there is no method to dump every alert at once.
 * Callers must walk the pages — that protects the database from a single rogue
 * plugin asking for thousands of rows in one query.
 */
public interface HistoryRepository {

    /**
     * Maximum number of entries returned in a single page. Always equal across alerts
     * and punishments and stable for the lifetime of a TotemGuard install.
     */
    int pageSize();

    /**
     * Returns a {@link HistoryView} bound to {@code uuid}. The view is cheap to create
     * and safe to retain — it holds no connection or page state of its own.
     */
    @NotNull HistoryView of(@NotNull UUID uuid);

    /**
     * Convenience overload of {@link #of(UUID)} for an already-resolved {@link TGUser}.
     */
    @NotNull HistoryView of(@NotNull TGUser user);

    /**
     * Removes every alert and punishment row for the target. Caches are invalidated
     * on completion so subsequent reads see an empty record.
     * <p>
     * The future settles with a {@link Result} carrying either the row counts
     * removed or a failure reason — it does not complete exceptionally for the database
     * being offline or a query failing.
     */
    @NotNull CompletableFuture<Result<HistoryClearResult>> clear(@NotNull UUID uuid);

    /**
     * Convenience overload of {@link #clear(UUID)}.
     */
    @NotNull CompletableFuture<Result<HistoryClearResult>> clear(@NotNull TGUser user);
}
