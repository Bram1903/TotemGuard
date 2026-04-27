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

import com.deathmotion.totemguard.api3.history.AlertEntry;
import com.deathmotion.totemguard.api3.history.HistoryPage;
import com.deathmotion.totemguard.api3.history.HistoryView;
import com.deathmotion.totemguard.api3.history.PunishmentEntry;
import com.deathmotion.totemguard.api3.result.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class HistoryViewImpl implements HistoryView {

    private final HistoryRepositoryImpl repository;
    private final UUID uuid;

    HistoryViewImpl(HistoryRepositoryImpl repository, UUID uuid) {
        this.repository = repository;
        this.uuid = uuid;
    }

    @Override
    public @NotNull UUID uuid() {
        return uuid;
    }

    @Override
    public int pageSize() {
        return repository.pageSize();
    }

    @Override
    public @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page) {
        return repository.alerts(uuid, page, null);
    }

    @Override
    public @NotNull CompletableFuture<Result<HistoryPage<AlertEntry>>> alerts(int page, @Nullable String checkName) {
        return repository.alerts(uuid, page, checkName);
    }

    @Override
    public @NotNull CompletableFuture<Result<Integer>> alertCount() {
        return repository.alertCount(uuid, null);
    }

    @Override
    public @NotNull CompletableFuture<Result<Integer>> alertCount(@Nullable String checkName) {
        return repository.alertCount(uuid, checkName);
    }

    @Override
    public @NotNull CompletableFuture<Result<HistoryPage<PunishmentEntry>>> punishments(int page) {
        return repository.punishments(uuid, page);
    }

    @Override
    public @NotNull CompletableFuture<Result<Integer>> punishmentCount() {
        return repository.punishmentCount(uuid);
    }
}
