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
 * Simple database status interface.
 *
 * <p>Allows consumers to check whether the database is enabled by configuration and
 * whether a connection is currently available. Useful for feature-gating: history,
 * statistics and the anti-VPN persistent cache all require a live database
 * connection.</p>
 *
 * <p>This interface does not expose any JDBC connection or DAO objects. It is
 * intended only for availability checks.</p>
 */
public interface DatabaseRepository {

    /**
     * Returns whether the database is enabled in the configuration.
     *
     * @return {@code true} if database support is enabled, {@code false} otherwise
     */
    boolean isEnabled();

    /**
     * Returns whether the database is currently connected and usable.
     *
     * <p>This is a best-effort snapshot of the current connection state.</p>
     *
     * @return {@code true} if the database appears connected, {@code false} otherwise
     */
    boolean isConnected();
}
