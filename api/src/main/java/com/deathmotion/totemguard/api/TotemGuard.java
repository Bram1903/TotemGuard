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

package com.deathmotion.totemguard.api;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static access point for the {@link TotemGuardAPI} instance. Initialized once via
 * {@link #init(TotemGuardAPI)}, accessed via {@link #get()} or {@link #getAsync()}.
 */
public final class TotemGuard {

    private static final AtomicReference<TotemGuardAPI> INSTANCE = new AtomicReference<>();
    private static final AtomicReference<CompletableFuture<TotemGuardAPI>> FUTURE =
            new AtomicReference<>(new CompletableFuture<>());

    private TotemGuard() {
        // utility class
    }

    /**
     * Initializes the global API instance for the first time.
     *
     * @throws IllegalStateException if already initialized. Use {@link #replace(TotemGuardAPI)}
     *                               when the loader is swapping the running plugin
     */
    public static void init(@NotNull TotemGuardAPI api) {
        Objects.requireNonNull(api, "api");

        if (!INSTANCE.compareAndSet(null, api)) {
            throw new IllegalStateException("TotemGuard API is already initialized.");
        }

        FUTURE.get().complete(api);
    }

    /**
     * Loader-internal. Replaces the current API instance with a freshly started one and
     * completes any future returned by {@link #getAsync()} after the previous
     * {@link #shutdown()}.
     */
    public static void replace(@NotNull TotemGuardAPI api) {
        Objects.requireNonNull(api, "api");
        INSTANCE.set(api);
        FUTURE.get().complete(api);
    }

    /**
     * Loader-internal. Clears the API instance and arms a fresh future. Called immediately
     * before {@link com.deathmotion.totemguard.api.event.events.TGPluginShutdownEvent} is
     * dispatched, so {@link #get()} throws inside a shutdown handler. Consumers needing the
     * API one last time during shutdown must cache their reference at startup.
     */
    public static void shutdown() {
        INSTANCE.set(null);
        FUTURE.set(new CompletableFuture<>());
    }

    /**
     * Returns the initialized API instance.
     *
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
     * Returns a future that completes when the API is initialized. After a {@link #shutdown()}
     * the future is reset, so consumers must re-call this method from their shutdown handler.
     */
    public static @NotNull CompletableFuture<TotemGuardAPI> getAsync() {
        TotemGuardAPI api = INSTANCE.get();
        return (api != null) ? CompletableFuture.completedFuture(api) : FUTURE.get();
    }

    /**
     * Whether {@link #init(TotemGuardAPI)} has run and {@link #shutdown()} has not been
     * called since. Use this to feature-gate code paths that depend on the API without
     * triggering the {@link #get()} throw.
     */
    public static boolean isInitialized() {
        return INSTANCE.get() != null;
    }
}
