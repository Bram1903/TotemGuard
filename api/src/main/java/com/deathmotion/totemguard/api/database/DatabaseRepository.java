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

package com.deathmotion.totemguard.api.database;

/**
 * Database availability status. Used to feature-gate history and statistics, which
 * require a live connection. Does not expose JDBC or DAO objects.
 */
public interface DatabaseRepository {

    /**
     * Whether the database is configured to start. Reflects the {@code database.enabled}
     * toggle, independent of whether the connection actually came up.
     */
    boolean isEnabled();

    /**
     * Best-effort snapshot of pool health. Cheap to call but lags by the pool's keepalive
     * interval, do not rely on it for strong "is the next query going to succeed" guarantees.
     */
    boolean isConnected();
}
