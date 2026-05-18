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
 * One slice of a paginated history result. Generic to keep the alert and punishment
 * surfaces aligned, callers can write the same loop over either.
 *
 * @param page         zero-based page index returned, capped at {@code totalPages - 1}.
 * @param pageSize     maximum number of entries per page; equal across all pages.
 * @param totalEntries total number of matching rows on the server side.
 * @param totalPages   number of pages the caller can iterate through ({@code >= 1}).
 * @param entries      the rows for {@code page}, ordered newest-first; never {@code null}.
 * @param <T>          {@link AlertEntry} or {@link PunishmentEntry}.
 */
public record HistoryPage<T>(
        int page,
        int pageSize,
        int totalEntries,
        int totalPages,
        @NotNull List<T> entries
) {

    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
