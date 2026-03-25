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

package com.deathmotion.totemguard.api3;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static access point for the {@link TotemGuardAPI} instance.
 * <p>
 * The API is initialized once via {@link #init(TotemGuardAPI)}. Consumers may access the
 * instance synchronously via {@link #get()} or asynchronously via {@link #getAsync()}.
 */
public final class TotemGuard {

    private static final AtomicReference<TotemGuardAPI> INSTANCE = new AtomicReference<>();
    private static final CompletableFuture<TotemGuardAPI> FUTURE = new CompletableFuture<>();

    private TotemGuard() {
        // utility class
    }

    /**
     * Initializes the global API instance exactly once.
     *
     * @param api the API instance
     * @throws NullPointerException  if {@code api} is null
     * @throws IllegalStateException if already initialized
     */
    public static void init(@NotNull TotemGuardAPI api) {
        Objects.requireNonNull(api, "api");

        if (!INSTANCE.compareAndSet(null, api)) {
            throw new IllegalStateException("TotemGuard API is already initialized.");
        }

        // Safe to call once; later calls are prevented by compareAndSet above.
        FUTURE.complete(api);
    }

    /**
     * Returns the initialized API instance.
     *
     * @return the API instance
     * @throws IllegalStateException if not initialized
     */
    public static @NotNull TotemGuardAPI get() {
        TotemGuardAPI api = INSTANCE.get();
        if (api == null) {
            throw new IllegalStateException(
                    "TotemGuard API is not initialized. Ensure TotemGuard.init(...) is called before accessing the API."
            );
        }
        return api;
    }

    /**
     * Returns a future that completes when the API is initialized.
     * <p>
     * If the API is already initialized, a completed future is returned.
     *
     * @return a future supplying the API instance
     */
    public static @NotNull CompletableFuture<TotemGuardAPI> getAsync() {
        TotemGuardAPI api = INSTANCE.get();
        return (api != null) ? CompletableFuture.completedFuture(api) : FUTURE;
    }

    /**
     * Returns whether the API has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return INSTANCE.get() != null;
    }
}
