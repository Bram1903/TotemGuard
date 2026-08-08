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

package com.deathmotion.totemguard.host;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * IOException subclass thrown by LoaderController methods that carries a classified
 * failure reason. Callers can {@code instanceof}-check and inspect {@link #reason()}
 * to decide whether to retry (transient network errors) or give up (no such version).
 * Without the reason, every failure looks identical from outside.
 */
public final class LoaderResolveException extends IOException {

    private final Reason reason;

    public LoaderResolveException(@NotNull Reason reason, @NotNull String message) {
        super(message);
        this.reason = reason;
    }

    public LoaderResolveException(@NotNull Reason reason, @NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public @NotNull Reason reason() {
        return reason;
    }

    public boolean isTransient() {
        return reason == Reason.UNREACHABLE || reason == Reason.UPSTREAM_ERROR;
    }

    public enum Reason {
        /**
         * Source could not be reached (DNS, timeout, refused). Retry later.
         */
        UNREACHABLE,
        /**
         * Source returned an error response (5xx, rate limit). Retry later.
         */
        UPSTREAM_ERROR,
        /**
         * Source answered, but no version matched the requested token. Don't retry.
         */
        NO_MATCH,
        /**
         * Resolved version is below the loader's minimum supported version. Don't retry.
         */
        GATE_REJECTED,
        /**
         * Generic failure (parse error, missing checksum, etc).
         */
        RESOLVE_FAILED
    }
}
