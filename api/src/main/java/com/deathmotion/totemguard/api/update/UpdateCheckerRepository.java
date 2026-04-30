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

package com.deathmotion.totemguard.api.update;

import com.deathmotion.totemguard.api.versioning.TGVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Tracks the latest published TotemGuard release.
 * <p>
 * The repository polls GitHub on its own schedule and shares results across
 * the server fleet via Redis (cache + pub/sub). Consumers typically just read
 * {@link #latestKnownVersion()} or {@link #isUpdateAvailable()}; calling
 * {@link #checkNow()} forces a fresh HTTP fetch and, when a new release is
 * discovered, propagates it to the rest of the fleet just like the periodic
 * check does.
 */
public interface UpdateCheckerRepository {

    /**
     * The most recent release this server is aware of, either fetched directly
     * or learned from a sibling server via Redis pub/sub.
     *
     * @return the latest known release, or {@code null} if no successful check
     * has completed yet
     */
    @Nullable TGVersion latestKnownVersion();

    /**
     * Convenience for {@code latestKnownVersion()} that returns {@code true}
     * only when a release strictly newer than the running plugin is known.
     *
     * @return {@code true} if an update is available
     */
    boolean isUpdateAvailable();

    /**
     * Force an HTTP request to the release source, bypassing the Redis cache.
     * <p>
     * On success the result is written back to the cache, broadcast to other
     * servers in the fleet, and applied to local state, identical to the
     * periodic check path. The returned stage completes with the freshly
     * fetched version, or {@code null} if the request failed (HTTP error,
     * network failure, malformed payload). The future never completes
     * exceptionally for those routine failures.
     * <p>
     * Concurrent callers receive the same in-flight future rather than each
     * triggering their own HTTP request.
     *
     * @return a future that completes when the fetch finishes
     */
    @NotNull CompletableFuture<@Nullable TGVersion> checkNow();
}
