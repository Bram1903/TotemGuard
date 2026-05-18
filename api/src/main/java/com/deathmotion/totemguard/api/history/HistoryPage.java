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

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * One slice of a paginated history result.
 *
 * @param page         zero-based page index this page represents, capped at {@code totalPages - 1}
 * @param pageSize     entries-per-page cap, identical to {@link HistoryRepository#pageSize()}
 *                     and equal across every page
 * @param totalEntries total matching rows across all pages (not just this page)
 * @param totalPages   page count, always at least {@code 1} even when {@code totalEntries == 0}
 * @param entries      rows for {@code page}, ordered newest-first by {@code created_at}
 * @param <T>          row projection, {@link AlertEntry} or {@link PunishmentEntry}
 */
public record HistoryPage<T>(
        int page,
        int pageSize,
        int totalEntries,
        int totalPages,
        @NotNull List<T> entries
) {

    /**
     * Whether a page after this one exists (i.e. {@code page + 1 < totalPages}).
     */
    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    /**
     * Whether a page before this one exists (i.e. {@code page > 0}).
     */
    public boolean hasPrevious() {
        return page > 0;
    }

    /**
     * Whether this page has no entries. {@code true} for empty results and pages past the end.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
