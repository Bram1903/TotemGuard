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

package com.deathmotion.totemguard.common.database;

public final class DatabaseTuning {

    public static final int POOL_MAX_SIZE = 8;
    public static final int POOL_MIN_IDLE = 2;
    public static final int POOL_CONNECTION_TIMEOUT_MS = 5_000;
    public static final int POOL_IDLE_TIMEOUT_MS = 60_000;
    public static final int POOL_MAX_LIFETIME_MS = 1_800_000;
    public static final int BATCH_MAX_SIZE = 100;
    public static final int BATCH_FLUSH_INTERVAL_MS = 500;
    public static final int BATCH_QUEUE_CAPACITY = 5_000;

    private DatabaseTuning() {
    }
}
