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

package com.deathmotion.totemguard.common.history;

import com.deathmotion.totemguard.api3.history.*;
import com.deathmotion.totemguard.api3.result.Result;
import com.deathmotion.totemguard.api3.result.ResultError;
import com.deathmotion.totemguard.api3.user.TGUser;
import com.deathmotion.totemguard.common.TGPlatform;
import com.deathmotion.totemguard.common.cache.CacheCodecs;
import com.deathmotion.totemguard.common.cache.CacheKeys;
import com.deathmotion.totemguard.common.cache.CacheRepositoryImpl;
import com.deathmotion.totemguard.common.database.DatabaseRepositoryImpl;
import com.deathmotion.totemguard.common.database.model.AlertCheckSummary;
import com.deathmotion.totemguard.common.database.model.AlertRecord;
import com.deathmotion.totemguard.common.database.model.PunishmentRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class HistoryRepositoryImpl implements HistoryRepository {

    public static final int PAGE_SIZE = 21;
    static final Duration PAGE_TTL = Duration.ofMinutes(2);
    static final Duration COUNT_TTL = Duration.ofMinutes(2);
    static final Duration SUMMARY_TTL = Duration.ofMinutes(2);
    // Outlives any page TTL so a clear stays sticky across the cluster.
    static final Duration VERSION_TTL = Duration.ofDays(7);

    private final TGPlatform platform;
    private final CacheRepositoryImpl cache;

    public HistoryRepositoryImpl() {
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

    private static <T> HistoryPage<T> buildPage(int page, int total, List<T> entries) {
        int safeTotal = Math.max(0, total);
        int totalPages = Math.max(1, (int) Math.ceil(safeTotal / (double) PAGE_SIZE));
        return new HistoryPage<>(page, PAGE_SIZE, safeTotal, totalPages, entries);
    }

    @Override
    public int pageSize() {
        return PAGE_SIZE;
    }

    @Override
    public @NotNull HistoryView of(@NotNull UUID uuid) {
        return new HistoryViewImpl(this, uuid);
    }

    @Override
    public @NotNull HistoryView of(@NotNull TGUser user) {
        return of(user.getUuid());
    }

    @Override
    public @NotNull CompletableFuture<Result<HistoryClearResult>> clear(@NotNull UUID uuid) {
        return supplyAsync(() -> {
            DatabaseRepositoryImpl db = platform.getDatabaseRepository();
            if (!db.isConnected()) return databaseUnavailable();
            try {
                long[] removed = db.deleteHistory(uuid);
                bumpVersion(uuid);
                return Result.ok(new HistoryClearResult(removed[0], removed[1]));
            } catch (SQLException ex) {
                return internalError("Failed to clear history", ex);
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Result<HistoryClearResult>> clear(@NotNull TGUser user) {
        return clear(user.getUuid());
    }

    public long versionFor(UUID uuid) {
        Long v = cache.getAndRefresh(CacheKeys.historyVersion(uuid), CacheCodecs.LONG, VERSION_TTL);
        return v == null ? 0L : v;
    }

    private void bumpVersion(UUID uuid) {
        cache.put(CacheKeys.historyVersion(uuid), System.currentTimeMillis(),
                CacheCodecs.LONG, VERSION_TTL);
    }

    public @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(UUID uuid, int rawPage, @Nullable String checkFilter) {
        int page = Math.max(0, rawPage);
        return supplyAsync(() -> {
            DatabaseRepositoryImpl db = platform.getDatabaseRepository();
            if (!db.isConnected()) return databaseUnavailable();

            long version = versionFor(uuid);
            String pageKey = CacheKeys.alertHistoryPage(uuid, version, page, checkFilter);
            String countKey = CacheKeys.alertHistoryCount(uuid, version, checkFilter);

            List<AlertRecord> records = cache.getAndRefresh(pageKey, CacheCodecs.ALERT_RECORDS, PAGE_TTL);
            Integer total = cache.getAndRefresh(countKey, CacheCodecs.INT, COUNT_TTL);

            try {
                if (records == null) {
                    records = checkFilter == null
                            ? db.findAlertsByPlayer(uuid, PAGE_SIZE, page * PAGE_SIZE)
                            : db.findAlertsByPlayerAndCheck(uuid, checkFilter, PAGE_SIZE, page * PAGE_SIZE);
                    cache.put(pageKey, records, CacheCodecs.ALERT_RECORDS, PAGE_TTL);
                }
                if (total == null) {
                    total = checkFilter == null
                            ? db.countAlertsByPlayer(uuid)
                            : db.countAlertsByPlayerAndCheck(uuid, checkFilter);
                    cache.put(countKey, total, CacheCodecs.INT, COUNT_TTL);
                }
            } catch (SQLException ex) {
                return internalError("Failed to load alert history", ex);
            }

            return Result.ok(buildPage(page, total, records.stream().map(HistoryMappers::toAlertEntry).toList()));
        });
    }

    public @NotNull CompletableFuture<Result<HistoryPage<PunishmentEntry>>> punishments(UUID uuid, int rawPage) {
        int page = Math.max(0, rawPage);
        return supplyAsync(() -> {
            DatabaseRepositoryImpl db = platform.getDatabaseRepository();
            if (!db.isConnected()) return databaseUnavailable();

            long version = versionFor(uuid);
            String pageKey = CacheKeys.punishmentHistoryPage(uuid, version, page);
            String countKey = CacheKeys.punishmentHistoryCount(uuid, version);

            List<PunishmentRecord> records = cache.getAndRefresh(pageKey, CacheCodecs.PUNISHMENT_RECORDS, PAGE_TTL);
            Integer total = cache.getAndRefresh(countKey, CacheCodecs.INT, COUNT_TTL);

            try {
                if (records == null) {
                    records = db.findPunishmentsByPlayer(uuid, PAGE_SIZE, page * PAGE_SIZE);
                    cache.put(pageKey, records, CacheCodecs.PUNISHMENT_RECORDS, PAGE_TTL);
                }
                if (total == null) {
                    total = db.countPunishmentsByPlayer(uuid);
                    cache.put(countKey, total, CacheCodecs.INT, COUNT_TTL);
                }
            } catch (SQLException ex) {
                return internalError("Failed to load punishment history", ex);
            }

            return Result.ok(buildPage(page, total, records.stream().map(HistoryMappers::toPunishmentEntry).toList()));
        });
    }

    public @NotNull CompletableFuture<Result<Integer>> alertCount(UUID uuid, @Nullable String checkFilter) {
        return supplyAsync(() -> {
            DatabaseRepositoryImpl db = platform.getDatabaseRepository();
            if (!db.isConnected()) return databaseUnavailable();

            long version = versionFor(uuid);
            String countKey = CacheKeys.alertHistoryCount(uuid, version, checkFilter);
            Integer total = cache.getAndRefresh(countKey, CacheCodecs.INT, COUNT_TTL);
            if (total != null) return Result.ok(total);

            try {
                int fresh = checkFilter == null
                        ? db.countAlertsByPlayer(uuid)
                        : db.countAlertsByPlayerAndCheck(uuid, checkFilter);
                cache.put(countKey, fresh, CacheCodecs.INT, COUNT_TTL);
                return Result.ok(fresh);
            } catch (SQLException ex) {
                return internalError("Failed to count alerts", ex);
            }
        });
    }

    public @NotNull CompletableFuture<Result<Integer>> punishmentCount(UUID uuid) {
        return supplyAsync(() -> {
            DatabaseRepositoryImpl db = platform.getDatabaseRepository();
            if (!db.isConnected()) return databaseUnavailable();

            long version = versionFor(uuid);
            String countKey = CacheKeys.punishmentHistoryCount(uuid, version);
            Integer total = cache.getAndRefresh(countKey, CacheCodecs.INT, COUNT_TTL);
            if (total != null) return Result.ok(total);

            try {
                int fresh = db.countPunishmentsByPlayer(uuid);
                cache.put(countKey, fresh, CacheCodecs.INT, COUNT_TTL);
                return Result.ok(fresh);
            } catch (SQLException ex) {
                return internalError("Failed to count punishments", ex);
            }
        });
    }

    public @NotNull CompletableFuture<Result<List<AlertCheckSummary>>> alertCheckSummaries(UUID uuid) {
        return supplyAsync(() -> {
            DatabaseRepositoryImpl db = platform.getDatabaseRepository();
            if (!db.isConnected()) return databaseUnavailable();

            long version = versionFor(uuid);
            String key = CacheKeys.alertHistoryCheckSummaries(uuid, version);
            List<AlertCheckSummary> cached = cache.getAndRefresh(key, CacheCodecs.ALERT_CHECK_SUMMARIES, SUMMARY_TTL);
            if (cached != null) return Result.ok(cached);

            try {
                List<AlertCheckSummary> fresh = db.findAlertCheckSummariesByPlayer(uuid);
                cache.put(key, fresh, CacheCodecs.ALERT_CHECK_SUMMARIES, SUMMARY_TTL);
                return Result.ok(fresh);
            } catch (SQLException ex) {
                return internalError("Failed to load alert summaries", ex);
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
