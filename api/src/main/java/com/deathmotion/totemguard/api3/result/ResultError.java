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

package com.deathmotion.totemguard.api3.result;

/**
 * Reason a {@link Result} came back as a failure. Match on this in your handler to
 * decide whether to surface a soft warning or a real error report. Shared by every
 * data-backed repository in the API (history, statistics, etc.).
 */
public enum ResultError {

    /**
     * The database is disabled in {@code config.yml} or is currently unreachable.
     * Treat as a soft failure; retry once the connection is back.
     */
    DATABASE_UNAVAILABLE,

    /**
     * A query reached the database but failed to complete (driver error, malformed
     * statement, timeout, etc.). The associated message contains the cause; this is
     * always a bug worth surfacing to the operator.
     */
    INTERNAL_ERROR
}
