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
import com.deathmotion.totemguard.common.database.dao.SchemaInfoDao.TableSize;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
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
        return Result.failure(ResultError.DATABASE_UNAVAILABLE);
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
                StatsSnapshot fresh = window.isAllTime() ? loadAllTime(db) : loadWindowed(db, window);
                cache.put(key, fresh, CacheCodecs.STATS_SNAPSHOT, STATS_TTL);
                return Result.ok(fresh);
            } catch (SQLException ex) {
                return internalError("Failed to load statistics", ex);
            }
        });
    }

    private StatsSnapshot loadAllTime(DatabaseRepositoryImpl db) throws SQLException {
        Map<String, TableSize> sizes = db.tableSizes();
        int alerts = estimatedRows(sizes, "tg_alerts");
        int punishments = estimatedRows(sizes, "tg_punishments");
        int uniquePlayers = db.countPlayersTotal();
        long bytes = computeBytes(sizes, alerts, punishments);
        return new StatsSnapshot(alerts, punishments, uniquePlayers, bytes);
    }

    private int estimatedRows(Map<String, TableSize> sizes, String tableName) {
        TableSize t = sizes.get(tableName);
        if (t == null) return 0;
        return (int) Math.min(t.rows(), Integer.MAX_VALUE);
    }

    private StatsSnapshot loadWindowed(DatabaseRepositoryImpl db, StatsWindow window) throws SQLException {
        Duration duration = Objects.requireNonNull(window.window(),
                "non-all-time StatsWindow must carry a duration");
        long since = System.currentTimeMillis() - duration.toMillis();

        Map<String, TableSize> sizes = db.tableSizes();
        int alerts = db.countAlertsSince(since);
        int punishments = db.countPunishmentsSince(since);
        int uniquePlayers = db.countPlayersActiveSince(since);
        long bytes = computeBytes(sizes, alerts, punishments);
        return new StatsSnapshot(alerts, punishments, uniquePlayers, bytes);
    }

    private long computeBytes(Map<String, TableSize> sizes, int alertsInWindow, int punishmentsInWindow) {
        long bytes = 0;
        for (TableSize t : sizes.values()) {
            long rows = switch (t.name()) {
                case "tg_alerts" -> alertsInWindow;
                case "tg_punishments" -> punishmentsInWindow;
                default -> t.rows();
            };
            bytes += t.avgRowLength() * Math.max(0L, rows);
        }
        return bytes;
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
