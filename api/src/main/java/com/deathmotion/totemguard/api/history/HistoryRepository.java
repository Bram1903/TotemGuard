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
 * Entry point for paginated alert and punishment history. Reads consult the cache chain
 * (memory, Redis, database), {@link #clear} invalidates every cached page for the target.
 * There is no bulk-fetch path so callers must walk pages.
 */
public interface HistoryRepository {

    /**
     * Page size cap for both alerts and punishments, stable for the lifetime of the install.
     */
    int pageSize();

    /**
     * A {@link HistoryView} bound to {@code uuid}. Cheap to create and safe to retain.
     */
    @NotNull HistoryView of(@NotNull UUID uuid);

    /**
     * Convenience overload of {@link #of(UUID)} that reads the UUID from {@code user}.
     */
    @NotNull HistoryView of(@NotNull TGUser user);

    /**
     * Removes every alert and punishment row for the target and invalidates caches. The
     * future never completes exceptionally for expected failures (database offline, query
     * error), inspect the {@link Result}.
     */
    @NotNull CompletableFuture<Result<HistoryClearResult>> clear(@NotNull UUID uuid);

    /**
     * Convenience overload of {@link #clear(UUID)} that reads the UUID from {@code user}.
     */
    @NotNull CompletableFuture<Result<HistoryClearResult>> clear(@NotNull TGUser user);
}
