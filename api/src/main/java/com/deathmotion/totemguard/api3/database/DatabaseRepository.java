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

package com.deathmotion.totemguard.api3.database;

/**
 * Public, read-only view of the TotemGuard database layer.
 *
 * <p>TotemGuard persists history to an external MySQL-compatible database
 * (MySQL 8+ or MariaDB). This interface exists today for feature-gating and
 * status reporting</p>
 */
public interface DatabaseRepository {

    /**
     * @return whether database persistence is enabled in the configuration
     */
    boolean isEnabled();

    /**
     * @return a best-effort snapshot of whether the connection pool is alive
     */
    boolean isConnected();
}
