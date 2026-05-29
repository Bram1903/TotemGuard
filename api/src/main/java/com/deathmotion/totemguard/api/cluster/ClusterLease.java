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

package com.deathmotion.totemguard.api.cluster;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * A named, fleet-wide lease used for single-owner coordination, for example deciding which
 * node runs a shared external connection. Acquired through
 * {@link ClusterService#acquireLease(String, Duration)}.
 *
 * <p>The lease auto-expires after its TTL, so the holder must {@link #renew(Duration)} before
 * it lapses. If renewal returns {@code false}, ownership was lost (the node stalled past the
 * TTL and another node took over) and the caller should relinquish whatever the lease guarded.
 */
public interface ClusterLease {

    /**
     * The lease name, as passed to {@code acquireLease}.
     */
    @NotNull String name();

    /**
     * Extends the lease for another {@code ttl}.
     *
     * @return {@code true} if this node still holds the lease and it was extended,
     * {@code false} if ownership was lost
     */
    boolean renew(@NotNull Duration ttl);

    /**
     * Releases the lease so another node can take it immediately. Best-effort and idempotent.
     * A lease that is not released simply expires after its TTL.
     */
    void release();
}
