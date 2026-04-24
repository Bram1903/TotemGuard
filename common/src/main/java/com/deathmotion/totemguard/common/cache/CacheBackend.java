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

package com.deathmotion.totemguard.common.cache;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface CacheBackend {

    boolean isAvailable();

    /**
     * Reads without touching the TTL.
     */
    @Nullable byte[] get(String key);

    /**
     * Reads and, on hit, resets the TTL to {@code ttl}.
     */
    @Nullable byte[] getAndRefresh(String key, Duration ttl);

    void put(String key, byte[] value, Duration ttl);

    void remove(String key);

    /**
     * @return {@code true} only if nothing was at {@code key}.
     */
    boolean putIfAbsent(String key, byte[] value, Duration ttl);
}
