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

package com.deathmotion.totemguard.common.stats;

import com.deathmotion.totemguard.api3.result.Result;
import com.deathmotion.totemguard.api3.result.ResultError;
import com.deathmotion.totemguard.api3.stats.StatsRepository;
import com.deathmotion.totemguard.api3.stats.StatsSnapshot;
import com.deathmotion.totemguard.api3.stats.StatsWindow;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class StatsRepositoryImpl implements StatsRepository {

    static final Duration STATS_TTL = Duration.ofMinutes(2);

    private final TGPlatform platform;
    private final CacheRepositoryImpl cache;

    public StatsRepositoryImpl() {
        this.platform = TGPlatform.getInstance();
        this.cache = platform.getCacheRepository();
    }

    private static <T> Result<T> databaseUnavailable() {
        return Result.failure(ResultError.DATABASE_UNAVAILABLE,
                "Database is disabled or currently unreachable");
    }

    private static <T> Result<T> internalError(String prefix, Throwable cause) {
        String detail = cause.getMessage();
        return Result.failure(ResultError.INTERNAL_ERROR,
                detail == null || detail.isBlank() ? prefix : prefix + ": " + detail);
    }

    @Override
    public @NotNull CompletableFuture<Result<StatsSnapshot>> snapshot(@NotNull StatsWindow window) {
        return supplyAsync(() -> {
            DatabaseRepositoryImpl db = platform.getDatabaseRepository();
            if (!db.isConnected()) return databaseUnavailable();

            String key = CacheKeys.statsSnapshot(window.id());
            StatsSnapshot cached = cache.getAndRefresh(key, CacheCodecs.STATS_SNAPSHOT, STATS_TTL);
            if (cached != null) return Result.ok(cached);

            try {
                int alerts;
                int punishments;
                if (window.isAllTime()) {
                    alerts = db.countAlertsTotal();
                    punishments = db.countPunishmentsTotal();
                } else {
                    long since = System.currentTimeMillis() - window.window().toMillis();
                    alerts = db.countAlertsSince(since);
                    punishments = db.countPunishmentsSince(since);
                }
                StatsSnapshot fresh = new StatsSnapshot(alerts, punishments);
                cache.put(key, fresh, CacheCodecs.STATS_SNAPSHOT, STATS_TTL);
                return Result.ok(fresh);
            } catch (SQLException ex) {
                return internalError("Failed to load statistics", ex);
            }
        });
    }

    private <T> CompletableFuture<T> supplyAsync(BlockingSupplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        platform.getScheduler().runAsyncTask(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable ex) {
                future.completeExceptionally(ex);
            }
        });
        return future;
    }

    @FunctionalInterface
    private interface BlockingSupplier<T> {
        T get() throws Exception;
    }
}
