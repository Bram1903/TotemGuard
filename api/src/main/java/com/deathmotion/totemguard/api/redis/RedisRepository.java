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

package com.deathmotion.totemguard.api.redis;

/**
 * Redis availability status. Does not expose client or connection objects, only used
 * for feature-gating and availability checks.
 */
public interface RedisRepository {

    /**
     * Whether Redis is configured to start. Reflects the {@code redis.enabled} toggle,
     * independent of whether the connection actually came up.
     */
    boolean isEnabled();

    /**
     * Best-effort snapshot of Lettuce's connection state. Cheap to call but lags by the
     * reconnect heartbeat, so do not treat a {@code true} as a guarantee that the next
     * call will succeed.
     */
    boolean isConnected();
}