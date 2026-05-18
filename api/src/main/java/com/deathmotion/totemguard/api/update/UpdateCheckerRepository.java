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
 * Tracks the latest published TotemGuard release. Polls GitHub on a schedule and shares
 * results across the fleet via Redis cache and pub/sub.
 */
public interface UpdateCheckerRepository {

    /**
     * Latest known release, or {@code null} if no successful check has completed yet.
     */
    @Nullable TGVersion latestKnownVersion();

    /**
     * Whether a release strictly newer than the running plugin is known.
     */
    boolean isUpdateAvailable();

    /**
     * Force a fresh HTTP fetch, bypassing the Redis cache. On success the result is
     * cached, broadcast to the fleet, and applied locally. Completes with {@code null}
     * for routine failures (HTTP error, network failure, malformed payload) rather than
     * exceptionally. Concurrent callers share one in-flight future.
     */
    @NotNull CompletableFuture<@Nullable TGVersion> checkNow();
}
