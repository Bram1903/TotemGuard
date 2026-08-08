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

package com.deathmotion.totemguard.api.fleet;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Distributed lock from {@link FleetCache#tryLock}. Auto-expires via Redis TTL even if
 * the holder dies, extend with {@link #refresh}.
 */
public interface FleetLock extends AutoCloseable {

    /**
     * Instance holding the lock. Equals {@link FleetCache#instanceId()} on the holder's side.
     */
    @NotNull UUID holder();

    /**
     * Wall-clock instant Redis will release the lock unless {@link #refresh} is called.
     */
    @NotNull Instant expiresAt();

    /**
     * Extends the TTL. Returns {@code false} if the lock already expired or was stolen.
     */
    boolean refresh(@NotNull Duration ttl);

    /**
     * Releases the lock. Safe to call multiple times.
     */
    @Override
    void close();
}
