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

package com.deathmotion.totemguard.api.loader;

import com.deathmotion.totemguard.api.host.LoaderInfo;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Programmatic control over the TotemGuard loader, surfaced to API consumers.
 *
 * <p>Only available when TotemGuard is running under the loader. Obtain an instance via
 * {@link com.deathmotion.totemguard.api.TotemGuardAPI#getLoaderControl()}, which returns
 * {@link java.util.Optional#empty()} for a standalone (non-loader) install. Every operation
 * is performed by the loader, not by the caller, so the caller never needs the loader's
 * internal classes.
 *
 * <p>Reloading configuration does <strong>not</strong> require the loader. Use
 * {@link com.deathmotion.totemguard.api.config.ConfigRepository#reloadAll()} for that.
 */
public interface LoaderControl {

    /**
     * An immutable snapshot of the loader's current state (configured source, loaded
     * version, staged version, and so on).
     *
     * @return the current loader info, never {@code null}
     */
    @NotNull LoaderInfo info();

    /**
     * Reloads (restarts) the managed TotemGuard plugin in place, without restarting the
     * Minecraft server. The previous classloader is discarded and a fresh instance is
     * started from the currently staged jar.
     *
     * <p>The returned future completes once the loader has finished restarting. It never
     * completes exceptionally for an expected failure. Transport problems are logged by the
     * loader.
     *
     * @return a future that completes when the restart finishes, never {@code null}
     */
    @NotNull CompletableFuture<Void> restart();

    /**
     * Resolves the newest jar from the loader's configured source, and if it differs from
     * the running version, downloads it, stages it, and restarts the plugin onto it.
     *
     * <p>This performs network I/O off the calling thread. The returned future always
     * completes normally with an {@link UpdateResult} describing the outcome (already up to
     * date, updating, or failed). It does not throw for expected failures such as a network
     * error.
     *
     * @return a future yielding the update outcome, never {@code null}
     */
    @NotNull CompletableFuture<UpdateResult> update();
}
